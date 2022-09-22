package net.judah.samples;

import static net.judah.util.AudioTools.mix;

import java.io.File;
import java.nio.FloatBuffer;

import javax.swing.JPanel;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.effects.Freeverb;
import net.judah.looper.AudioTrack;
import net.judah.looper.WavFile;
import net.judah.util.Constants;

/** currently, plays ((crickets)) on 2 and 4 */
public class Sample extends AudioTrack {

	@Getter protected File file;
	private SamplePad samplePad;
	
	private final float[] workL = new float[bufferSize];
    private final float[] workR = new float[bufferSize];
    private final FloatBuffer bufL = FloatBuffer.wrap(workL);
    private final FloatBuffer bufR = FloatBuffer.wrap(workR);
	
	/** load preset by name (without .wav) */
	public Sample(JackPort left, JackPort right, String wavName, Type type) throws Exception {
		this(left, right, wavName, new File(Constants.SAMPLES, wavName + ".wav"), type);
	}
	
	public Sample(JackPort left, JackPort right, String name, File f, Type type) throws Exception {
		this(name, type, left, right);
		this.file = f;
		setRecording(WavFile.load(file));
		gain.setVol(95);
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

	
	@Override
	public void process() {
		if (!active || !hasRecording()) return;
		readRecordedBuffer();
		playFrame(bufL, bufR); 
    }
	
	@Override
	protected void doReverb(FloatBuffer inL, FloatBuffer inR) {
		FloatBuffer outL = leftPort.getFloatBuffer().rewind();
		FloatBuffer outR = rightPort.getFloatBuffer().rewind();
		if (reverb.isActive()) {
			((Freeverb)reverb).process(inL, inR);
		}
		mix(inL, outL);
		mix(inR, outR);
	}
	
	public JPanel getPad() {
		if (samplePad == null)
			samplePad = new SamplePad(this);
		return samplePad;
	}
}
