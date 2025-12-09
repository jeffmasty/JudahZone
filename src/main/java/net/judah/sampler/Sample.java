package net.judah.sampler;

import java.io.File;

import net.judah.omni.Recording;
import net.judah.util.Folders;

/** Audio from disk */
public class Sample extends BasicPlayer {

	protected static final float BOOST = 0.125f;

	@Override
	public void play(boolean onOrOff) {
		playing = onOrOff;
	}

	/** load preset by name (without .wav) */
	public Sample(String wavName, Type type) throws Exception {
		this(wavName, new File(Folders.getSamples(), wavName + ".wav"), type);
	}

	public Sample(String name, File f, Type type) throws Exception {
		this.type = type;
		file = f;
		recording = new Recording(f);
		gain.setPan(0.4999f);
	}

}
