package net.judah.drumkit;

import java.io.File;
import java.nio.FloatBuffer;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.looper.AudioTrack;
import net.judah.midi.Actives;

@Getter
public class DrumSample extends AudioTrack implements AtkDec {

	private final DrumEnvelope envelope;
	private final DrumType drumType;
	private final Actives actives;
	protected File file;
	@Setter private int attackTime = 1;
	@Setter private int decayTime = Integer.MAX_VALUE;
	@Setter protected float velocity = 1f;

	public DrumSample(DrumType type, Actives actives) {
		super(type.name());
		this.drumType = type;
		this.actives = actives;
		envelope = new DrumEnvelope(this);
	}

	public void setFile(File f) throws Exception {
		this.file = f;
		setRecording(DrumDB.get(f));
	}
	
	public void process(FloatBuffer outLeft, FloatBuffer outRight) {
		if (!playing) return;
		readRecordedBuffer();
		if (onMute)
			return;
		env = 2 * velocity * envelope.calcEnv();
		playFrame(outLeft, outRight);
	}

	@Override public void clear() { // not used
        tapeCounter.set(0);
        recording = null;
        playing = false;
        MainFrame.update(this);
    }

	public void off() {
		playing = false;
		ShortMessage m = actives.find(drumType.getData1());
		if (m != null)
			actives.remove(m);
		MainFrame.update(actives);
	}
}
