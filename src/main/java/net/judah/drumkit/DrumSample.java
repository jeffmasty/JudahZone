package net.judah.drumkit;

import java.io.File;
import java.nio.FloatBuffer;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.looper.AudioTrack;

@Getter
public class DrumSample extends AudioTrack implements AtkDec {

	private final DrumEnvelope envelope;
	private final DrumType drumType;
	private final GMDrum gmDrum;
	protected File file;
	@Setter private int attackTime = 1;
	@Setter private int decayTime = 1000;
	@Setter protected float velocity = 1f;

	public DrumSample(DrumType type) {
		super(type.name());
		this.drumType = type;
		this.gmDrum = GMDrum.lookup(drumType.getData1());
		envelope = new DrumEnvelope(this);
	}

	public void setFile(File f) throws Exception {
		this.file = f;
		setRecording(DrumDB.get(f));
	}
	
	public void process(FloatBuffer outLeft, FloatBuffer outRight) {
		if (!active) return;
		if (hasRecording()) {
			readRecordedBuffer();
			if (onMute)
				return;
			env = 2 * velocity * envelope.calcEnv();
			playFrame(outLeft, outRight);
    	} 	
	}

	@Override public void clear() {
        tapeCounter.set(0);
        recording = null;
        length = null;
        recording = null;
        active = false;
        MainFrame.update(this);
    }
	
}
