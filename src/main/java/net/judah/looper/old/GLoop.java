package net.judah.looper.old;

import static net.judah.looper.old.GLoop.Mode.*;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.JackException;

import lombok.Getter;
import net.judah.jack.AudioClient;
import net.judah.jack.ClientConfig;
import net.judah.jack.mixers.AdvancedMixer;
import net.judah.jack.mixers.Merge;
import net.judah.jack.mixers.NoiseCancellingMixer;
import net.judah.jack.mixers.SimpleMixer;
import net.judah.looper.Memory;

public abstract class GLoop extends AudioClient {
    public static enum Mode {NEW, ARMED, STARTING, RUNNING, STOPPING, STOPPED};

	protected final Looper looper;
	@Getter protected final LoopSettings settings;
	ArrayList<Listener> listeners = new ArrayList<>();
    protected final Merge[] mixers = new Merge[] {new SimpleMixer(), new NoiseCancellingMixer(), new AdvancedMixer()};
    protected Merge mixer = mixers[0];
    @Getter protected AtomicReference<Mode> recording = new AtomicReference<Mode>(NEW);
    @Getter protected AtomicReference<Mode> playback = new AtomicReference<Mode>(NEW);
//	protected final AtomicInteger counter = new AtomicInteger();
	@Getter protected long sync;
	@Getter protected float gain = 1f;
	protected int loopCount;

	protected Memory memory;

	public GLoop(Looper looper, ClientConfig config, LoopSettings settings) throws JackException {
		super(config);
		this.looper = looper;
		this.settings = settings;
	}

	@Override
	protected void initialize() throws JackException {
		super.initialize();
		memory = new Memory(outputs.size(), buffersize);
		// TODO exercise process() in JIT (for xruns)
	}

	public void setGain(int gain) {
		if (gain <= 150 && gain >= 0) {
			this.gain = gain * 0.01f;
			LooperUI.instance.addText("gain: " + this.gain);
		}
	}

	public boolean isMaster() {
		return this.getClass().equals(MasterLoop.class);
	}

	public boolean isSlave() {
		return this.getClass().equals(SlaveLoop.class);
	}

	public void register(Listener listener) {
		listeners.add(listener);
	}

	//////////////////////////////
	// Control interface
	public void setPlay(boolean active) {
		// Mode current = playback.get();

		if (active) {
			if (recording.get().equals(NEW)) {
				playback.set(ARMED);
			}
			else {
				playback.compareAndSet(NEW, STARTING);
				playback.compareAndSet(STOPPED, STARTING);
			}
		}
		else {
			playback.compareAndSet(RUNNING, STOPPING);
			playback.compareAndSet(ARMED, NEW);
		}
		// log.debug("Play ? " + active + " from " + current + " to " + playback.get() );
	}

	public void setRecord(boolean active) {
		if (active) {
			recording.compareAndSet(NEW, STARTING);
			recording.compareAndSet(STOPPED, STARTING);
		} else {
			recording.compareAndSet(RUNNING, STOPPING);
		}
	}

	public abstract void clear();

//	protected abstract void recordingFinished(long time);
    ////////////////////////////////////////////////////
    //                PROCESS AUDIO                   //
    ////////////////////////////////////////////////////
//	/** put recording in the out buffer
//	 * Play https://github.com/kampfschlaefer/jackmix/blob/master/backend/jack_backend.cpp*/
//	public void play(List<FloatBuffer> outputs, int nframes) {
//		float[][] old = next().getInputs();
//		FloatBuffer b;
//		for (int i = 0; i < outputs.size(); i++) {
//			b = outputs.get(i);
//			b.rewind();
//			for (int j = 0; j < nframes; j++) {
//				b.put(old[i][j] * gain);
//			}
//		}
//	}

}
