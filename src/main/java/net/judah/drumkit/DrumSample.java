package net.judah.drumkit;

import static net.judah.util.Constants.LEFT;
import static net.judah.util.Constants.RIGHT;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.PlayAudio;
import net.judah.api.Recording;
import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.midi.Actives;
import net.judah.util.AudioTools;
import net.judah.util.Constants;

@Getter
public class DrumSample implements PlayAudio {

	protected final FloatBuffer left = FloatBuffer.wrap(new float[Constants.bufSize()]);
    protected final FloatBuffer right = FloatBuffer.wrap(new float[Constants.bufSize()]);

	private final DrumEnvelope envelope;
	private final DrumType drumType;
	private final Gain gain;
	private final Actives actives;
	@Setter protected float velocity = 1f;
	@Setter protected boolean onMute;

	protected File file;
	protected boolean playing;
	protected Recording recording = new Recording();
	protected final AtomicInteger tapeCounter = new AtomicInteger(0);

	protected float[][] playBuffer;
	protected float env = 1f; // envelope/boost

	public DrumSample(DrumType type, Actives actives, KitSetup setup) {
		this.drumType = type;
		this.actives = actives;
		this.gain = setup.gain[type.ordinal()];
		this.envelope = new DrumEnvelope(setup, type.ordinal());
	}

	public void setFile(File f) throws Exception {
		this.file = f;
		setRecording(DrumDB.get(f));
	}

	@Override
	public void rewind() {
		tapeCounter.set(0);
	}

	@Override
	public void setRecording(Recording sample) {
		rewind();
		recording = sample;
	}

	@Override public void play(boolean play) {
		this.playing = play;
	}

	public void reset() { // not used
        tapeCounter.set(0);
        playing = false;
    }

	public void off() {
		playing = false;
		ShortMessage m = actives.find(drumType.getData1());
		if (m != null)
			actives.remove(m);
		MainFrame.update(actives);
	}

	protected void readRecordedBuffer() {
		int frame = tapeCounter.getAndIncrement();
		if (frame + 1 >= recording.size()) {
			tapeCounter.set(0);
			off();
		}
		playBuffer = recording.get(frame);
	}

	@Override public int getLength() {
		return recording.size();
	}

	@Override public final float seconds() {
		return getLength() / Constants.fps();
	}

	public void clear() {
        setRecording(null);
        playing = false;
        file = null;
    }

	public void process(FloatBuffer outLeft, FloatBuffer outRight) {
		if (!playing) return;
		readRecordedBuffer();
		if (onMute)
			return;
		env = 2 * velocity * envelope.calcEnv();
		AudioTools.replace(playBuffer[LEFT], left, env * gain.getLeft() * gain.getGain());
		AudioTools.replace(playBuffer[RIGHT], right, env * gain.getRight() * gain.getGain());
		// No FX stream().filter(fx->fx.isActive()).forEach(fx->fx.process(left, right));
		AudioTools.mix(left, outLeft);
		AudioTools.mix(right, outRight);
	}

}

//protected final Filter filter = new Filter(true, Filter.Type.Band, 120);
//protected final Overdrive overdrive = new Overdrive();
//add(filter);
//add(overdrive);

