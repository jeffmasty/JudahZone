package net.judah.looper;

import static net.judah.util.Constants.LEFT;
import static net.judah.util.Constants.RIGHT;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.PlayAudio;
import net.judah.mixer.Channel;
import net.judah.omni.AudioTools;
import net.judah.omni.Recording;
import net.judah.util.Constants;

@Getter
public abstract class AudioTrack extends Channel implements PlayAudio {
	protected final int bufSize = Constants.bufSize();

	@Setter protected Type type = Type.ONE_SHOT;
	protected boolean playing;
	protected Recording recording = new Recording();
	protected final AtomicInteger tapeCounter = new AtomicInteger(0);
	protected float[][] playBuffer;
	protected float env = 1f; // envelope/boost

	public AudioTrack(String name, Type type) {
		super(name, true);
		this.type = type;
	}

	public AudioTrack(String name) {
		this(name, Type.ONE_SHOT);
	}

	public abstract void process();

	@Override public void play(boolean play) {
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


	// env and playBuffer pre-calculated
	protected void playFrame(FloatBuffer outLeft, FloatBuffer outRight) {

		// I am a channel, my iterator is actives

		// forEach()-> activeFx.process(left, right);

		AudioTools.replace(playBuffer[LEFT], left, env * gain.getLeft());
		AudioTools.replace(playBuffer[RIGHT], right, env * gain.getRight());

		filter1.process(left, right);
		filter2.process(left, right);

		if (chorus.isActive())
			chorus.processStereo(left, right);

		if (compression.isActive()) {
			compression.process(left);
			compression.process(right);
		}

		if (overdrive.isActive()) {
			overdrive.processAdd(left);
			overdrive.processAdd(right);
		}

		if (eq.isActive()) {
			eq.process(left, true);
			eq.process(right, false);
		}

		if (delay.isActive()) {
			delay.process(left, right);
		}
		if (reverb.isActive())
			reverb.process(left, right);

		// gain & stereo pan to provided buffer
		AudioTools.mix(left, outLeft);
		AudioTools.mix(right, outRight);
	}

	@Override public final void rewind() {
		tapeCounter.set(0);
	}

	@Override public final float seconds() {
		return getLength() / Constants.fps();
	}

}
