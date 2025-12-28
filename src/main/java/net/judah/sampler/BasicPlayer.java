package net.judah.sampler;

import static net.judah.util.Constants.LEFT;
import static net.judah.util.Constants.RIGHT;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.PlayAudio;
import net.judah.api.Recording;
import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.util.AudioTools;
import net.judah.util.Constants;

@Getter
public class BasicPlayer implements PlayAudio {

	protected final AtomicInteger tapeCounter = new AtomicInteger(0);
	protected final Gain gain = new Gain();
	protected boolean playing;
	protected Recording recording = new Recording();
	protected File file;
	@Setter protected float env = 0.125f;
	@Setter protected Type type = Type.ONE_SHOT;

	@Override public void play(boolean onOrOff) {
		this.playing = onOrOff;
	}

	public final void clear() {
		playing = false;
        setRecording(null);
        file = null;
    }

	@Override public final String toString() {
		if (file == null)
			return "---";
		return file.getName().replace(".wav", "");
	}

	@Override public final int getLength() {
		return recording.size();
	}

	@Override public final float seconds() {
		return getLength() / Constants.fps();
	}

	@Override public final void rewind() {
		tapeCounter.set(0);
	}

	@Override public final void setRecording(Recording sample) {
		rewind();
		recording = sample;
		MainFrame.update(this);
	}

	public void process(FloatBuffer outLeft, FloatBuffer outRight) {
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
