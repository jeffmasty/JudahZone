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
import net.judah.mixer.EffectsGui;
import net.judah.mixer.LoopGui;
import net.judah.mixer.MixerBus;
import net.judah.util.Console;

@Log4j
public class Sample extends MixerBus implements ProcessAudio, TimeNotifier {
	
	private final int bufferSize = JudahMidi.getInstance().getBufferSize();
	
    @Getter protected Recording recording; 
	@Getter @Setter protected Type type;
	@Getter protected final List<JackPort> outputPorts = new ArrayList<>();

	@Getter protected final AtomicInteger tapeCounter = new AtomicInteger();
	@Getter protected final AtomicReference<AudioMode> isPlaying = new AtomicReference<AudioMode>(STOPPED);
	@Getter @Setter protected boolean timeSync = false;
	@Getter private int loopCount = 0;
	
	@Setter @Getter protected float gain = 1f;
	@Setter @Getter private float gainFactor = 2f;
	@Getter protected Integer length;
	protected final ArrayList<TimeListener> listeners = new ArrayList<>();
	protected LoopGui gui;
	private float unmute = -1f;

	// for process()
	private int updated; // tape position counter
	private FloatBuffer toJackLeft, toJackRight;
	protected float[][] recordedBuffer;
	private final float[] workL = new float[bufferSize];
	private final float[] workR = new float[bufferSize];

	public Sample(String name, Recording recording, Type type) {
		super(name);
		this.recording = recording;
		length = recording.size();
	}
	
	protected Sample() {super("?"); }
	
	@Override
	public void setOutputPorts(List<JackPort> ports) {
		synchronized (outputPorts) {
			outputPorts.clear();
			outputPorts.addAll(ports);
			getGui();
		}
	}
	
	@Override
	public void setOnMute(boolean mute) {
		if (mute) {
			unmute = gain;
			gain = 0f;
		}
		else {
			gain = unmute;
			unmute = -1f;
		}
	}
	@Override
	public boolean isOnMute() {
		return unmute != -1;
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
	}
	public boolean hasRecording() {
		return recording != null && length != null && length > 0;
	}

	protected final boolean playing() {
		isPlaying.compareAndSet(STOPPING, STOPPED);
		isPlaying.compareAndSet(STARTING, RUNNING);
		return isPlaying.get() == RUNNING;
	}
	
	@Override
	public void clear() {
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

	@Override
	public String toString() {
		return "Sample " + name;
	}

	@Override
	public LoopGui getGui() {
		if (gui == null)
			gui = new LoopGui(this);
		return gui;
	}
	
	@Override
	public void addListener(TimeListener l) {
		if (!listeners.contains(l))
			listeners.add(l);
	}

	@Override
	public void removeListener(TimeListener l) {
		listeners.remove(l);
	}

	public void setTapeCounter(int current) {
		tapeCounter.set(current);
	}
	
	/** percent of maximum */
	@Override
	public void setVolume(int volume) {
		gain = volume / 100f * gainFactor;
		EffectsGui.volume(this);
		gui.setVolume(volume);
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
	
	////////////////////////////////////
	//     Process Realtime Audio     //
	////////////////////////////////////
	@Override
	public void process() {
		
		if (!playing()) return;
		
		// output
		toJackLeft = outputPorts.get(LEFT_CHANNEL).getFloatBuffer();
		toJackLeft.rewind();
		toJackRight = outputPorts.get(RIGHT_CHANNEL).getFloatBuffer();
		toJackRight.rewind();
		readRecordedBuffer();
		
		if (eq.isActive()) {
			AudioTools.processGain(recordedBuffer[LEFT_CHANNEL], workL, gain);
			AudioTools.processGain(recordedBuffer[RIGHT_CHANNEL], workR, gain);
			
			eq.process(FloatBuffer.wrap(workL), FloatBuffer.wrap(workR), 1);
			
			if (compression.isActive()) {
				compression.process(workL, toJackLeft, 1);
				compression.process(workR, toJackRight, 1);
			}
			else {
				processMix(workL, toJackLeft, 1);
				processMix(workR, toJackRight, 1);
			}
		}
		else if (compression.isActive()) {
			compression.process(recordedBuffer[LEFT_CHANNEL], toJackLeft, gain);
			compression.process(recordedBuffer[RIGHT_CHANNEL], toJackRight, gain);
		}
		else {
			processMix(recordedBuffer[LEFT_CHANNEL], toJackLeft, gain);
			processMix(recordedBuffer[RIGHT_CHANNEL], toJackRight, gain);
		}
		// TODO reverb
	}

	@Override
	public int getVolume() {
		return Math.round(gain * 100);
	}

}
