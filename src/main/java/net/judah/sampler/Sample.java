package net.judah.sampler;

import java.io.File;

import net.judah.omni.Recording;
import net.judah.util.Folders;

/** Audio from disk */
public class Sample extends BasicPlayer {

	protected static final float BOOST = 0.125f;
	protected final Sampler sampler;

	@Override
	public void play(boolean onOrOff) {
		playing = onOrOff;
//		MainFrame.update(this);
	}

	/** load preset by name (without .wav) */
	public Sample(String wavName, Type type, Sampler sampler) throws Exception {
		this(wavName, new File(Folders.getSamples(), wavName + ".wav"), type, sampler);
	}

	public Sample(String name, File f, Type type, Sampler sampler) throws Exception {
		this.sampler = sampler;
		this.type = type;
		this.file = f;
		recording = new Recording(file);
	}

}
