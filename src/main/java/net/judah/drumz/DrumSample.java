package net.judah.drumz;

import java.io.File;
import java.nio.FloatBuffer;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.effects.CutFilter;
import net.judah.effects.Freeverb;
import net.judah.looper.AudioTrack;
import net.judah.looper.WavFile;
import net.judah.tracker.GMDrum;
import net.judah.util.AudioTools;
import net.judah.util.Constants;

public class DrumSample extends AudioTrack {

	@Getter private final DrumType drumType;
	@Getter private final GMDrum gmDrum;
	private final CutFilter hiCut = new CutFilter(true);
	
	@Getter protected File file;
	private FloatBuffer workL = FloatBuffer.allocate(Constants.bufSize());
	private FloatBuffer workR = FloatBuffer.allocate(Constants.bufSize());
	
	public DrumSample(DrumType type) {
		super(type.name());
		this.drumType = type;
		this.gmDrum = type.getDat();
		this.type = Type.ONE_SHOT;
		gain.setGain(0.10f);
	}

	public void setFile(File f) throws Exception {
		setRecording(WavFile.load(f));
		this.file = f;
	}
	
	@Override
	public void process() {
	}
	
	public void process(FloatBuffer[] buf) {
		if (!active) return;
		if (hasRecording()) {
			readRecordedBuffer();
    		if (!onMute) {
    			AudioTools.silence(workL);
    			AudioTools.silence(workR);
    			hiCut.process(workL, workR, 1);
    			playFrame(workL, workR);
    			AudioTools.mix(workL, buf[0]);
    			AudioTools.mix(workR, buf[1]);
    		}
    	} 	
	}

	@Override
	protected void doReverb(FloatBuffer inL, FloatBuffer inR) {
		if (reverb.isActive()) {
			((Freeverb)reverb).process(inL, inR);
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
