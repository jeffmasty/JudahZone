package net.judah.looper;

import static net.judah.util.Constants.LEFT;
import static net.judah.util.Constants.RIGHT;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.PlayAudio;
import net.judah.fx.Effect;
import net.judah.mixer.Channel;
import net.judah.omni.AudioTools;
import net.judah.omni.Recording;
import net.judah.util.Constants;

/** plays something already recorded */
@Getter
public abstract class AudioTrack extends Channel implements PlayAudio {

	@Setter protected Type type = Type.ONE_SHOT;
	protected boolean playing;
	protected Recording recording = new Recording();
	protected final AtomicInteger tapeCounter = new AtomicInteger(0);
	protected float[][] playBuffer;

	public AudioTrack(String name, Type type) {
		super(name, true);
		this.type = type;
	}

	public AudioTrack(String name) {
		this(name, Type.ONE_SHOT);
	}

	@Override public final void play(boolean play) {
		this.playing = play;
	}

	@Override public void setRecording(Recording sample) {
		rewind();
		recording = sample;
	}

	// Loop overrides
	@Override public int getLength() {
		return recording.size();
	}

	@Override public final void rewind() {
		tapeCounter.set(0);
	}

	@Override public final float seconds() {
		return getLength() / Constants.fps();
	}

	/** run active effects on the current frame being played */
	public final void fx(FloatBuffer outLeft, FloatBuffer outRight) {

		AudioTools.replace(playBuffer[LEFT], left, gain.getLeft());
		AudioTools.replace(playBuffer[RIGHT], right, gain.getRight());

		stream().filter(Effect::isActive).forEach(fx -> fx.process(left, right));
		AudioTools.mix(left, outLeft);
		AudioTools.mix(right, outRight);
	}

}
