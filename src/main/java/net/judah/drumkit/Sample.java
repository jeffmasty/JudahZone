package net.judah.drumkit;

import java.io.File;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.looper.AudioTrack;
import net.judah.looper.Recording;
import net.judah.util.Folders;

/** currently, plays ((crickets)) on 2 and 4 */
public class Sample extends AudioTrack {
	
	protected static final float BOOST = 0.25f;
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

	@Override public void clear() {
        tapeCounter.set(0);
        recording = null;
        length = null;
        recording = null;
        active = false;
        MainFrame.update(this);
        file = null;
    }
	
	public void process() {
		if (!active || !hasRecording()) return;
		readRecordedBuffer();
		env = BOOST * sampler.mix;
		playFrame(leftPort.getFloatBuffer(), rightPort.getFloatBuffer()); 
    }
	
}
