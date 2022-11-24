package net.judah.drumz;

import java.io.File;
import java.nio.FloatBuffer;

import lombok.Getter;
import lombok.Setter;
import net.judah.MainFrame;
import net.judah.looper.AudioTrack;
import net.judah.tracker.GMDrum;
import net.judah.util.AudioTools;
import net.judah.util.Constants;

@Getter
public class DrumSample extends AudioTrack implements AtkDec {

	private final DrumEnvelope envelope;
	private final DrumType drumType;
	private final GMDrum gmDrum;
	protected File file;
	@Setter private int attackTime = 1;
	@Setter private int decayTime = 100;
	
	protected final FloatBuffer[] buffer = new FloatBuffer[] 
			{FloatBuffer.allocate(Constants.bufSize()), FloatBuffer.allocate(Constants.bufSize())};
	
	public DrumSample(DrumType type) {
		super(type.name());
		this.drumType = type;
		this.gmDrum = type.getDat();
		this.type = Type.ONE_SHOT;
		envelope = new DrumEnvelope(this);
		
	}

	public void setFile(File f) throws Exception {
		setRecording(DrumDB.get(f));
		this.file = f;
	}
	
	public void process(FloatBuffer[] output) {
		AudioTools.silence(buffer);
		if (!active) return;
		if (hasRecording()) {
			readRecordedBuffer();
    		if (!onMute) {
    			env = envelope.calcEnv();
    			playFrame(buffer, output[0], output[1]);
    		}
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
