package net.judah.looper;



import static net.judah.jack.AudioMode.*;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.jack.AudioMode;
import net.judah.jack.ProcessAudio;
import net.judah.midi.MidiClient;
import net.judah.mixer.Mixer;
import net.judah.mixer.MixerPort;
import net.judah.util.JudahException;

@Log4j
public class Sample implements ProcessAudio {
	
    @Getter protected Recording recording; 
	@Getter @Setter protected String name;
	@Getter @Setter protected Type type;
	
	@Setter protected transient List<MixerPort> outputPorts;
	@Getter protected final transient AtomicInteger tapeCounter = new AtomicInteger();
	protected final transient AtomicReference<AudioMode> isPlaying = new AtomicReference<AudioMode>(STOPPED);
	
	@Setter @Getter protected transient float gain = 1f;
	
	// for process()
	protected transient float[][] recordedBuffer;
	private transient int updated; // tape position counter
	private transient final float[] workArea = new float[MidiClient.getInstance().getBuffersize()];
	private transient FloatBuffer toJackLeft, toJackRight;
	private transient int z;

	public Sample(String name, Recording recording, Type type) {
		this.name = name;
		this.recording = recording;
	}
	
	public Sample(String name, Recording recording) throws JudahException {
		this(name, recording, Type.ONE_TIME);
	}
	
	public Sample(String name, Recording recording, List<MixerPort> outputPorts, int bufSize) throws JudahException {
		this.name = name;
		this.recording = recording;
		this.outputPorts = outputPorts;
	}
	
	protected Sample() { }
	
	protected float[][] getCurrent() {
		return recording.get(tapeCounter.get());
	}

	@Override public final AudioMode isPlaying() {
		return isPlaying.get();
	}
	
	/** sets the playing flag (actual start/stop happens in the Jack thread) */
	@Override
	public void play(boolean active) {
		log.warn(name + " playing active: " + active);
		
		if (isPlaying.compareAndSet(NEW, active ? ARMED : NEW)) {
			if (active) log.warn("Play is armed.");
		}
		isPlaying.compareAndSet(ARMED, active ? ARMED : hasRecording() ? STOPPED : NEW);
		isPlaying.compareAndSet(RUNNING, active ? RUNNING : STOPPING);
		
		if (isPlaying.compareAndSet(STOPPED, active ? STARTING : STOPPED)) {
			if (active) 
				log.warn("playing starting. sample has " + recording.size() + " buffers.");
		}
	}
	public boolean hasRecording() {
		return recording != null && !recording.isEmpty();
	}

	public void setRecording(Recording sample) {
		recording = sample;
		log.warn("Recording loaded");
	}
	
	/** for process() thread */
	private void updateCounter() {
		updated = tapeCounter.get() + 1;
		if (updated == recording.size()) {
			if (type == Type.ONE_TIME) {
				isPlaying.set(STOPPING);
				new Thread() {
					@Override public void run() {Mixer.getInstance().removeSample(Sample.this);}
				}.start();
			}
			updated = 0;
		}
		tapeCounter.set(updated);
	}
	
	private final boolean playing() {
		isPlaying.compareAndSet(STOPPING, STOPPED);
		isPlaying.compareAndSet(STARTING, RUNNING);
		return isPlaying.get() == RUNNING;
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
			recordedBuffer = getCurrent();
			processMix(recordedBuffer[LEFT_CHANNEL], toJackLeft, nframes);
			processMix(recordedBuffer[RIGHT_CHANNEL], toJackRight, nframes);
			updateCounter();
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
	public void clear() {
		log.warn("clearing " + name);
		boolean wasRunning = false;
		if (isPlaying.compareAndSet(RUNNING, STOPPING)) {
			wasRunning = true;
			try { // to get a process() in
				Thread.sleep(20); } catch (Exception e) {	}
		}
		isPlaying.set(wasRunning ? ARMED : NEW);
		
		tapeCounter.set(0);
		recording = null;
	}

	@Override
	public String toString() {
		return "Sample " + name;
	}
}
