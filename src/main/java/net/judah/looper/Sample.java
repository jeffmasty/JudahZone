package net.judah.looper;

import static net.judah.jack.AudioMode.*;
import static net.judah.jack.AudioTools.*;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.api.TimeListener;
import net.judah.api.TimeListener.Property;
import net.judah.api.TimeNotifier;
import net.judah.jack.AudioMode;
import net.judah.jack.AudioTools;
import net.judah.jack.ProcessAudio;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.util.Console;

@Log4j
public class Sample extends Channel implements ProcessAudio, TimeNotifier {
	
	private final int bufferSize = JudahMidi.getInstance().getBufferSize();
	
    @Getter protected Recording recording; 
	@Getter protected final List<JackPort> outputPorts = new ArrayList<>();
    @Setter @Getter protected Type type;
	@Setter @Getter protected float gain = 1f;
	@Setter @Getter private float gainFactor = 2f;
	@Getter protected final AtomicInteger tapeCounter = new AtomicInteger();
	@Getter protected final AtomicReference<AudioMode> isPlaying = new AtomicReference<AudioMode>(STOPPED);
	@Getter private int loopCount = 0;
	@Getter protected Integer length;
	
	protected final ArrayList<TimeListener> listeners = new ArrayList<>();
	/** TODO synchronized loops or Time Master sync.. */
	@Getter @Setter protected boolean sync = false;
	
	// for process()
	private int updated; // tape position counter
	private FloatBuffer toJackLeft, toJackRight;
	protected float[][] recordedBuffer;
	private final float[] workL = new float[bufferSize];
	private final float[] workR = new float[bufferSize];
	private final FloatBuffer bufL = FloatBuffer.wrap(workL);
	private final FloatBuffer bufR = FloatBuffer.wrap(workR);

	public Sample(String name, Recording recording, Type type) {
		super(name);
		this.recording = recording;
		length = recording.size();
	}
	
	protected Sample() {super("?"); }
	
	@Override public void setOutputPorts(List<JackPort> ports) {
		synchronized (outputPorts) {
			outputPorts.clear();
			outputPorts.addAll(ports);
		}
	}
	
	public int getSize() {
		if (recording == null) return 0;
		return recording.size();
	}
	
	@Override public final AudioMode isPlaying() {
		return isPlaying.get();
	}
	
	/** sets the playing flag (actual start/stop happens in the Jack thread) */
	@Override
	public void play(boolean active) {
		log.trace(name + " playing active: " + active);
		
		if (isPlaying.compareAndSet(NEW, active ? ARMED : NEW)) {
			if (active) Console.info(name + " Play is armed.");
		}
		isPlaying.compareAndSet(ARMED, active ? ARMED : hasRecording() ? STOPPED : NEW);
		isPlaying.compareAndSet(RUNNING, active ? RUNNING : STOPPING);
		
		if (isPlaying.compareAndSet(STOPPED, active ? STARTING : STOPPED)) {
			if (active) 
				Console.info(name + " playing. sample has " + recording.size() + " buffers.");
		}
		if (gui != null) gui.update();
	}
	public boolean hasRecording() {
		return recording != null && length != null && length > 0;
	}

	@Override public void clear() {
		boolean wasRunning = false;
		if (isPlaying.compareAndSet(RUNNING, STOPPING)) {
			wasRunning = true;
			try { // to get a process() in
				Thread.sleep(20); } catch (Exception e) {	}
		}
		isPlaying.set(wasRunning ? ARMED : NEW);
		tapeCounter.set(0);
		recording = null;
		length = null;
		Console.info(name + " flushed.");
	}

	public void setRecording(Recording sample) {
		if (recording != null)
			recording.close();
		recording = sample;
		length = recording.size();
		Console.info("Recording loaded, " + length + " frames.");
		isPlaying.set(STOPPED);
	}

	@Override public String toString() {
		return "Sample " + name;
	}

	@Override public void addListener(TimeListener l) {
		if (!listeners.contains(l))
			listeners.add(l);
	}

	@Override public void removeListener(TimeListener l) {
		listeners.remove(l);
	}

	public void setTapeCounter(int current) {
		tapeCounter.set(current);
	}
	
	/** percent of maximum */
	@Override public void setVolume(int volume) {
		gain = volume / 100f * gainFactor;
		super.setVolume(volume);
	}

	@Override public int getVolume() {
		return Math.round(gain * 100);
	}

	////////////////////////////////////
	//     Process Realtime Audio     //
	////////////////////////////////////
	@Override
	public void process() {
		
		if (!playing()) return;
		
		readRecordedBuffer();
	
		if (isOnMute()) return; // ok, we're on mute and we've progressed the tape counter.

		// gain
		AudioTools.processGain(recordedBuffer[LEFT_CHANNEL], workL, gain);
		AudioTools.processGain(recordedBuffer[RIGHT_CHANNEL], workR, gain);
		
		if (eq.isActive()) {
			eq.process(workL, true);
			eq.process(workR, false);
		}	
		
		if (compression.isActive()) {
			compression.process(workL, FloatBuffer.wrap(workL), 1);
			compression.process(workR, FloatBuffer.wrap(workR), 1);
		}
		if (getDelay().isActive()) {
			delay.processAdd(bufL, bufL, true, 1);
			delay.processAdd(bufR, bufR, false, 1);
		}
		if (cutFilter.isActive()) {
			cutFilter.process(bufL, bufR, 1);
		}
		// TODO reverb (current Reverb algorithm isn't working well for output)
		
		// output
		toJackLeft = outputPorts.get(LEFT_CHANNEL).getFloatBuffer();
		toJackLeft.rewind();
		toJackRight = outputPorts.get(RIGHT_CHANNEL).getFloatBuffer();
		toJackRight.rewind();
		processMix(workL, toJackLeft);
		processMix(workR, toJackRight);
		
	}
	
	protected final boolean playing() {
		isPlaying.compareAndSet(STOPPING, STOPPED);
		isPlaying.compareAndSet(STARTING, RUNNING);
		return isPlaying.get() == RUNNING;
	}

	protected void readRecordedBuffer() {
		
		recordedBuffer = recording.get(tapeCounter.get());

		updated = tapeCounter.get() + 1;
		if (updated == recording.size()) {
			if (type == Type.ONE_TIME) {
				isPlaying.set(STOPPING);
				new Thread() { @Override public void run() {
					JudahZone.getLooper().remove(Sample.this);
				}}.start();
			}
			updated = 0;
			loopCount++;
			new Thread() { @Override public void run() {
				for (int i = 0; i < listeners.size(); i++)
					listeners.get(i).update(Property.LOOP, loopCount);	
			}}.start();
		}
		tapeCounter.set(updated);
	}
	

}
