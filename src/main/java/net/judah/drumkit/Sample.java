package net.judah.drumkit;

import java.io.File;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.looper.AudioTrack;
import net.judah.looper.Recording;
import net.judah.util.Folders;

/** currently, plays ((crickets)) on 2 and 4 */
public class Sample extends AudioTrack {
	
	private static final float BOOST = 0.2f;
	@Getter protected File file;
	@Getter @Setter protected float mix = 0.5f;
	
	/** load preset by name (without .wav) */
	public Sample(JackPort left, JackPort right, String wavName, Type type) throws Exception {
		this(left, right, wavName, new File(Folders.getSamples(), wavName + ".wav"), type);
	}
	
	public Sample(JackPort left, JackPort right, String name, File f, Type type) throws Exception {
		this(name, type, left, right);
		this.file = f;
		setRecording(new Recording(file));
		env = BOOST; // boost
	}

	/** blank sample */
	public Sample(String name, Type type, JackPort left, JackPort right) {
		super(name);
		leftPort = left;
		rightPort = right;
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
		env = BOOST * mix;
		playFrame(leftPort.getFloatBuffer(), rightPort.getFloatBuffer()); 
    }
	
}
