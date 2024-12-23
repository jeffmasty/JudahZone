package net.judah.sampler;

import java.io.File;
import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.looper.AudioTrack;
import net.judah.omni.Recording;
import net.judah.util.Folders;

/** Audio from disk */
public class Sample extends AudioTrack {

	protected static final float BOOST = 0.125f;
	protected final Sampler sampler;
	@Getter protected File file;

	/** load preset by name (without .wav) */
	public Sample(JackPort left, JackPort right, String wavName, Type type, Sampler sampler) throws Exception {
		this(left, right, wavName, new File(Folders.getSamples(), wavName + ".wav"), type, sampler);
	}

	public Sample(JackPort left, JackPort right, String name, File f, Type type, Sampler sampler) throws Exception {
		this(name, type, left, right, sampler);
		this.file = f;
		setRecording(new Recording(file));
	}

	/** blank sample */
	public Sample(String name, Type type, JackPort left, JackPort right, Sampler sampler) {
		super(name);
		leftPort = left;
		rightPort = right;
		this.sampler = sampler;
		this.type = type;
	}

	public Sample(FloatBuffer left, FloatBuffer right, File f) throws Exception {
		super(f.getName().replace(".wav", ""));
		sampler = null;
		setRecording(new Recording(f, 1));
	}

	@Override public void clear() {
        tapeCounter.set(0);
        recording = null;
        playing = false;
        MainFrame.update(this);
        file = null;
    }

	@Override
	public void process() {
		if (!playing) return;
		env = BOOST * sampler.mix;
		readSampleBuffer();
		playFrame(leftPort.getFloatBuffer(), rightPort.getFloatBuffer());
    }

	protected void readSampleBuffer() {
		int frame = tapeCounter.getAndIncrement();
		if (frame + 1 >= recording.size()) {
			tapeCounter.set(0);
			if (type == Type.ONE_SHOT) {
				playing = false;
				MainFrame.update(this);
			}
		}
		playBuffer = recording.get(frame);
	}

}
