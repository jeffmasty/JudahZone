package net.judah.sampler;

import java.io.File;
import java.io.IOException;

import net.judah.util.Folders;
import net.judah.util.Recording;

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
		recording = Recording.loadInternal(f);
	}

	public Sample(String wavName, File f, Type oneShot, float gain) throws IOException {
		this.type = oneShot;
		file = f;
		recording = Recording.loadInternal(f, gain);
	}

}
