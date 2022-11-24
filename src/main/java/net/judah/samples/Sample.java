package net.judah.samples;

import java.io.File;
import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.looper.AudioTrack;
import net.judah.looper.Recording;
import net.judah.util.Constants;

/** currently, plays ((crickets)) on 2 and 4 */
public class Sample extends AudioTrack {

	@Getter protected File file;
	private SamplePad samplePad;
	
	private final float[] workL = new float[bufSize];
    private final float[] workR = new float[bufSize];
	private FloatBuffer[] buffer = new FloatBuffer[] {FloatBuffer.wrap(workL), FloatBuffer.wrap(workR)};
    
	/** load preset by name (without .wav) */
	public Sample(JackPort left, JackPort right, String wavName, Type type) throws Exception {
		this(left, right, wavName, new File(Constants.SAMPLES, wavName + ".wav"), type);
	}
	
	public Sample(JackPort left, JackPort right, String name, File f, Type type) throws Exception {
		this(name, type, left, right);
		this.file = f;
		setRecording(new Recording(file));
		env = 2f; // boost
	}

	@Override public void clear() {
        tapeCounter.set(0);
        recording = null;
        length = null;
        recording = null;
        active = false;
        MainFrame.update(this);
    }
	
	/** blank sample */
	public Sample(String name, Type type, JackPort left, JackPort right) {
		super(name);
		leftPort = left;
		rightPort = right;
		this.type = type;
	}

	
	public void process() {
		if (!active || !hasRecording()) return;
		readRecordedBuffer();
		playFrame(buffer, leftPort.getFloatBuffer(), rightPort.getFloatBuffer()); 
    }
	
	public SamplePad getPad() {
		if (samplePad == null)
			samplePad = new SamplePad(this);
		return samplePad;
	}
}
