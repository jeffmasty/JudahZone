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
import net.judah.fx.Filter;
import net.judah.fx.Gain;
import net.judah.fx.Overdrive;
import net.judah.gui.MainFrame;
import net.judah.looper.Recording;
import net.judah.midi.Actives;
import net.judah.util.AudioTools;
import net.judah.util.Constants;

@Getter
public class DrumSample implements AtkDec, PlayAudio {

	protected final FloatBuffer left = FloatBuffer.allocate(Constants.bufSize());
	protected final FloatBuffer right = FloatBuffer.allocate(Constants.bufSize());
	private final DrumEnvelope envelope;
	private final DrumType drumType;
	private final Actives actives;
	protected File file;
	@Setter private int attackTime = 1;
	@Setter private int decayTime = Integer.MAX_VALUE;
	@Setter protected float velocity = 1f;
	@Setter protected boolean onMute;
	protected boolean playing; 
	protected Recording recording = new Recording();
	protected final AtomicInteger tapeCounter = new AtomicInteger(0);
	protected float[][] playBuffer;
	protected float env = 1f; // envelope/boost
	
    protected final Gain gain = new Gain();
	protected final Filter filter = new Filter(true, Filter.Type.Band, 120);
    protected final Overdrive overdrive = new Overdrive();
	
	public DrumSample(DrumType type, Actives actives) {
		this.drumType = type;
		this.actives = actives;
		envelope = new DrumEnvelope(this);
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

	public void process(FloatBuffer outLeft, FloatBuffer outRight) {
		if (!playing) return;
		readRecordedBuffer();
		if (onMute)
			return;
		env = 2 * velocity * envelope.calcEnv();
		playFrame(outLeft, outRight);
	}

	public void reset() { // not used
        tapeCounter.set(0);
        playing = false;
        overdrive.setActive(false);
        gain.setPan(0.5f);
        filter.setActive(false);
    }

	public void off() {
		playing = false;
		ShortMessage m = actives.find(drumType.getData1());
		if (m != null)
			actives.remove(m);
		MainFrame.update(actives);
	}
	
	/**@return 0 to 100*/
	public int getVolume() {
		return gain.get(Gain.VOLUME);
	}
	
	/**@return 0 to 100*/
	public int getPan() {
		return gain.get(Gain.PAN);
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

	@Override public void clear() {
        setRecording(null);
        playing = false;
        file = null;
    }

	protected void playFrame(FloatBuffer outLeft, FloatBuffer outRight) {

		AudioTools.replace(playBuffer[LEFT], left, env * gain.getLeft());
		AudioTools.replace(playBuffer[RIGHT], right, env * gain.getRight());
		
		filter.process(left, right);
		if (overdrive.isActive()) {
			overdrive.processAdd(left);
			overdrive.processAdd(right);
		}

		// gain & stereo pan to provided buffer
		AudioTools.mix(left, outLeft);
		AudioTools.mix(right, outRight);
	}
	
	
}
