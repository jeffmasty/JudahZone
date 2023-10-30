package net.judah.looper;

import static net.judah.util.Constants.LEFT;
import static net.judah.util.Constants.RIGHT;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.PlayAudio;
import net.judah.drumkit.DrumSample;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;
import net.judah.util.AudioTools;
import net.judah.util.Constants;

@Getter
public abstract class AudioTrack extends Channel implements PlayAudio {

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

	@Override public void play(boolean play) {
		this.playing = play;
	}

	@Override public void setRecording(Recording sample) {
		rewind();
		recording = sample;
	}

	@Override public int getLength() {
		return recording.size();
	}
	
	// not for loops
	protected void readRecordedBuffer() {
		int frame = tapeCounter.getAndIncrement();
		if (frame + 1 >= recording.size()) {
			tapeCounter.set(0);
			if (this instanceof DrumSample) {
				((DrumSample)this).off();
			}
			else if (type == Type.ONE_SHOT) {
				playing = false;
				MainFrame.update(this);
			}
		}
		playBuffer = recording.get(frame);
	}

	protected void playFrame(FloatBuffer outLeft, FloatBuffer outRight) {

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
			delay.process(left, right, true);
			delay.process(left, right, false);
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

	@Override public String toString() {
		return this.getClass().getSimpleName() + " " + name;
	}

}
