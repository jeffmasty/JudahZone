package net.judah.looper;

import static net.judah.jack.AudioMode.*;
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
import net.judah.jack.ProcessAudio;
import net.judah.midi.JudahMidi;
import net.judah.mixer.LoopGui;
import net.judah.util.Console;

@Log4j
public class Sample implements ProcessAudio, TimeNotifier {
	
    @Getter protected Recording recording; 
	@Getter @Setter protected String name;
	@Getter @Setter protected Type type;
	protected LoopGui gui;
	
	protected final transient List<JackPort> outputPorts = new ArrayList<>();
	@Getter protected final transient AtomicInteger tapeCounter = new AtomicInteger();
	protected final ArrayList<TimeListener> listeners = new ArrayList<>();
	protected final transient AtomicReference<AudioMode> isPlaying = new AtomicReference<AudioMode>(STOPPED);
	@Getter @Setter protected boolean timeSync = false;
	private int loopCount = 0;
	
	@Setter @Getter protected transient float gain = 1f;
	@Setter @Getter private float gainFactor = 2f;
	private float unmute = -1f;
	
	protected Integer length;
	
	// for process()
	protected transient float[][] recordedBuffer;
	private transient int updated; // tape position counter
	private transient final float[] workArea = new float[JudahMidi.getInstance().getBufferSize()];
	private transient FloatBuffer toJackLeft, toJackRight;
	private transient int z;

	public Sample(String name, Recording recording, Type type) {
		this.name = name;
		this.recording = recording;
		length = recording.size();
	}
	
	protected Sample() { }
	
	@Override
	public void setOutputPorts(List<JackPort> ports) {
		synchronized (outputPorts) {
			outputPorts.clear();
			outputPorts.addAll(ports);
		}
	}
	
	public void mute(boolean mute) {
		if (mute) {
			unmute = gain;
			gain = 0f;
		}
		else {
			gain = unmute;
			unmute = -1f;
		}
	}
	
	public int getSize() {
		if (recording == null) return 0;
		return recording.size();
	}
	
	protected void readRecordedBuffer() {
		recordedBuffer = recording.get(tapeCounter.get());
		
		updated = tapeCounter.get() + 1;
		if (updated == recording.size()) {
			if (type == Type.ONE_TIME) {
				isPlaying.set(STOPPING);
				JudahZone.getLooper().remove(Sample.this);
			}
			updated = 0;
			loopCount++;
			new Thread() {
				@Override public void run() {
					for (int i = 0; i < listeners.size(); i++)
						listeners.get(i).update(Property.LOOP, loopCount);	
			}}.start();
			
		}
		tapeCounter.set(updated);
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
	public void setVolume(int volume) {
		gain = volume / 100f * gainFactor;
		if (gui != null) {
			gui.setVolume(volume);
		}
	}


	////////////////////////////////////
	//     Process Realtime Audio     //
	////////////////////////////////////
	@Override
	public void process(int nframes) {
		
		// output
		if (playing()) {
			toJackLeft = outputPorts.get(LEFT_CHANNEL).getFloatBuffer();
			toJackLeft.rewind();
			toJackRight = outputPorts.get(RIGHT_CHANNEL).getFloatBuffer();
			toJackRight.rewind();
			readRecordedBuffer();
			processMix(recordedBuffer[LEFT_CHANNEL], toJackLeft, nframes);
			processMix(recordedBuffer[RIGHT_CHANNEL], toJackRight, nframes);
		} 
	}

	protected void processMix(float[] in, FloatBuffer out, int nframes) {
		out.get(workArea);
		out.rewind();
		for (z = 0; z < nframes; z++) {
			out.put(workArea[z] + gain * in[z]);
		}
	}

}
