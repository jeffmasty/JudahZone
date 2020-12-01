package net.judah.looper;



import static net.judah.jack.AudioMode.*;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
import net.judah.mixer.MixerPort;
import net.judah.util.Console;

@Log4j
public class Sample implements ProcessAudio, TimeNotifier {
	
    @Getter protected Recording recording; 
	@Getter @Setter protected String name;
	@Getter @Setter protected Type type;
	
	@Setter protected transient List<MixerPort> outputPorts;
	@Getter protected final transient AtomicInteger tapeCounter = new AtomicInteger();
	@Getter @Setter boolean timeSync = false;
	private final ArrayList<TimeListener> listeners = new ArrayList<>();
	private int loopCount = 0;
	
	protected final transient AtomicReference<AudioMode> isPlaying = new AtomicReference<AudioMode>(STOPPED);
	
	@Setter @Getter protected transient float gain = 1f;
	protected Integer length;
	
	// for process()
	protected transient float[][] recordedBuffer;
	private transient int updated; // tape position counter
	private transient final float[] workArea = new float[JudahMidi.getInstance().getBuffersize()];
	private transient FloatBuffer toJackLeft, toJackRight;
	private transient int z;

	
	public Sample(String name, Recording recording, Type type) {
		this.name = name;
		this.recording = recording;
		length = recording.size();
	}
	
	protected Sample() { }
	
	protected void readRecordedBuffer() {
		recordedBuffer = recording.get(tapeCounter.get());
		
		updated = tapeCounter.get() + 1;
		if (updated == recording.size()) {
			if (type == Type.ONE_TIME) {
				isPlaying.set(STOPPING);
				new Thread() {
					@Override public void run() {JudahZone.getCurrentSong().getMixer().removeSample(Sample.this);}
				}.start();
			}
			updated = 0;
			
			listeners.forEach(listener -> {listener.update(Property.LOOP, ++loopCount);});
			//	if (timeSync) JudahZone.getCurrentSong().pulse();
		}
		tapeCounter.set(updated);
	}

	@Override public final AudioMode isPlaying() {
		return isPlaying.get();
	}
	
	/** sets the playing flag (actual start/stop happens in the Jack thread) */
	@Override
	public void play(boolean active) {
		log.warn(name + " playing active: " + active);
		
		if (isPlaying.compareAndSet(NEW, active ? ARMED : NEW)) {
			if (active) Console.info(name + " Play is armed.");
		}
		isPlaying.compareAndSet(ARMED, active ? ARMED : hasRecording() ? STOPPED : NEW);
		isPlaying.compareAndSet(RUNNING, active ? RUNNING : STOPPING);
		
		if (isPlaying.compareAndSet(STOPPED, active ? STARTING : STOPPED)) {
			if (active) 
				Console.addText(name + " playing. sample has " + recording.size() + " buffers.");
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
		Console.addText("clearing " + name);
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
	}

	public void setRecording(Recording sample) {
		if (recording != null)
			recording.close();
		recording = sample;
		length = recording.size();
		Console.addText("Recording loaded, " + length + " frames.");
		isPlaying.set(STOPPED);
	}

	@Override
	public String toString() {
		return "Sample " + name;
	}

	////////////////////////////////////
	//     Process Realtime Audio     //
	////////////////////////////////////
	@Override
	public void process(int nframes) {
		
		// output
		if (playing()) {
			toJackLeft = outputPorts.get(LEFT_CHANNEL).getPort().getFloatBuffer();
			toJackLeft.rewind();
			toJackRight = outputPorts.get(RIGHT_CHANNEL).getPort().getFloatBuffer();
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

	@Override
	public void addListener(TimeListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(TimeListener l) {
		listeners.remove(l);
	}

	public void setTapeCounter(int current) {
		tapeCounter.set(current);
	}
	
}
