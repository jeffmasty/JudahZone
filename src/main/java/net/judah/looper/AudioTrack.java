package net.judah.looper;

import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.AudioMode;
import net.judah.api.ProcessAudio;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;
import net.judah.util.AudioTools;

@Getter
public abstract class AudioTrack extends Channel implements ProcessAudio {

	protected Recording recording = new Recording();
	protected float[][] recordedBuffer;
	@Setter protected boolean active;
	@Setter protected Type type = Type.ONE_SHOT;
	protected final AtomicInteger tapeCounter = new AtomicInteger();
	@Setter protected Integer length;
	protected float env = 1f;

	public AudioTrack(String name) {
		this(name, Type.ONE_SHOT);
	}

	public AudioTrack(String name, Type type) {
		super(name, true);
		this.type = type;
	}

	public boolean hasRecording() {
		return length != null && length > 1 && recording != null;
	}

	public void setRecording(Recording sample) {
		if (recording != null)
			recording.close();
		recording = sample;
		length = recording.size();
	}

	@Override
	public final AudioMode isPlaying() {
		return active ? AudioMode.RUNNING : AudioMode.ARMED;
	}

	@Override // Loop has more sophisticated version
	public void readRecordedBuffer() {
		int updated = tapeCounter.get();
		recordedBuffer = recording.get(updated);
		if (++updated == recording.size()) {
			updated = 0;
			if (type == Type.ONE_SHOT) {
				active = false;
				MainFrame.update(this);
			}
		}
		tapeCounter.set(updated);
	}

	protected void playFrame(FloatBuffer outLeft, FloatBuffer outRight) {

		AudioTools.replace(recordedBuffer[LEFT_CHANNEL], left, env * gain.getLeft());
		AudioTools.replace(recordedBuffer[RIGHT_CHANNEL], right, env * gain.getRight());
		
		party.process(left, right);
		filter.process(left, right);

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
			delay.processAdd(left, right, true);
			delay.processAdd(left, right, false);
		}
		if (reverb.isActive())
			reverb.process(left, right);

		// gain & stereo pan to provided buffer
		AudioTools.mix(left, outLeft);
		AudioTools.mix(right, outRight);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " " + name;
	}

	@Override
	public final void setTapeCounter(int current) {
		tapeCounter.set(current);
	}

}
