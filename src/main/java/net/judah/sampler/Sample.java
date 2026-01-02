package net.judah.sampler;

import static judahzone.util.Constants.LEFT;
import static judahzone.util.Constants.RIGHT;

import java.io.File;
import java.io.IOException;

import judahzone.fx.Gain;
import judahzone.jnajack.BasicPlayer;
import judahzone.util.AudioTools;
import judahzone.util.Folders;
import judahzone.util.Recording;
import lombok.Getter;
import net.judah.gui.MainFrame;

/** Audio from disk */
public class Sample extends BasicPlayer {

	protected static final float BOOST = 0.125f;
	@Getter protected final Gain gain = new Gain();

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

	@Override // see BasicPlayer
	public void process(float[] outLeft, float[] outRight) {
		if (!playing) return;

		int frame = tapeCounter.getAndIncrement();
		if (frame + 1 >= recording.size()) {
			tapeCounter.set(0);
			if (type == Type.ONE_SHOT) {
				playing = false;
				MainFrame.update(this);
			}
		}
		if (!playing)
			return;

		float[][] buf = recording.get(frame);

		AudioTools.mix(buf[LEFT], env * gain.getLeft() * gain.getGain(), outLeft);
		AudioTools.mix(buf[RIGHT], env * gain.getRight() * gain.getGain(), outRight);
	}
}
