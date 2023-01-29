package net.judah.drumkit;

import java.io.File;
import java.nio.FloatBuffer;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.looper.AudioTrack;
import net.judah.util.AudioTools;
import net.judah.util.Constants;

@Getter
public class DrumSample extends AudioTrack implements AtkDec {

	private final DrumEnvelope envelope;
	private final DrumType drumType;
	private final GMDrum gmDrum;
	protected File file;
	@Setter private int attackTime = 1;
	@Setter private int decayTime = 1000;
	
	@Setter protected float velocity = 1f;

	
	protected final FloatBuffer[] buffer = new FloatBuffer[] 
			{FloatBuffer.allocate(Constants.bufSize()), FloatBuffer.allocate(Constants.bufSize())};
	
	public DrumSample(DrumType type) {
		super(type.name());
		this.drumType = type;
		this.gmDrum = GMDrum.lookup(drumType.getData1());
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
    			env = velocity * envelope.calcEnv();
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
