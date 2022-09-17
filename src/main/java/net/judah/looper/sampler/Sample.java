package net.judah.looper.sampler;

import java.io.File;

import lombok.Getter;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.looper.WavFile;
import net.judah.util.Constants;

/** currently, plays ((crickets)) on 2 and 4 */
public class Sample extends Loop {

	@Getter final protected File file;
	@Getter final SamplePad pad = new SamplePad(this);
	
	public Sample(Looper l, String wavName, Type type) throws Exception {
		this(l, wavName, new File(Constants.SAMPLES, wavName + ".wav"), type);
	}
	
	public Sample(Looper l, String name, File f, Type type) throws Exception {
		super(name, l);
		this.file = f;
		setRecording(WavFile.load(file));
		this.type = type; 
		gain.setVol(85);
	}

	@Override
	public void process() {
		if (!active) return;
    	if (hasRecording()) {
    		readRecordedBuffer();
    		playFrame();
    	} 	
    }
	
	@Override
		public void setActive(boolean active) {
			super.setActive(active);
			pad.update();
		}
	
}
