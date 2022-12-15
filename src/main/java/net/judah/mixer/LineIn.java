package net.judah.mixer;

import java.nio.FloatBuffer;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.widgets.GuitarTuner;

@Getter
public abstract class LineIn extends Channel {
    protected boolean muteRecord;
    @Setter protected boolean solo;
    protected LatchEfx latchEfx = new LatchEfx(this);
    /** set to <code>null</code> for no processing */
    @Setter protected GuitarTuner tuner;
    
    public LineIn(String name, boolean stereo) {
    	super(name, stereo);
    }
    
    public void setMuteRecord(boolean muteRecord) {
		this.muteRecord = muteRecord;
		MainFrame.update(this);
	}

	public void processFx(FloatBuffer mono) {
		mono.rewind();
		if (this == GuitarTuner.getChannel()){
			MainFrame.update(AudioTools.copy(mono));
			mono.rewind();
		}
		float gain = getVolume() * 0.5f;
		for (int z = 0; z < Constants.bufSize(); z++)
			mono.put(mono.get(z) * gain);

		hiCut.process(mono);
		cutFilter.process(mono);
		if (eq.isActive()) {
			eq.process(mono, true);
		}
		if (chorus.isActive()) {
			chorus.processMono(mono);
		}
		if (overdrive.isActive()) {
			overdrive.processAdd(mono);
		}
		if (delay.isActive()) {
			delay.processAdd(mono, mono, true);
		}
		if (compression.isActive())
			compression.process(mono);
		if (reverb.isActive() && reverb.isInternal()) {
			reverb.process(mono);
		}
	}
	
	public void processFx(FloatBuffer left, FloatBuffer right, float amplification) {
		left.rewind();
		right.rewind();
		
		if (this == GuitarTuner.getChannel()) {
			MainFrame.update(AudioTools.copy(left));
			left.rewind();
		}
		
		for (int z = 0; z < Constants.bufSize(); z++) {
			left.put(left.get(z) * amplification);
			right.put(right.get(z) * amplification);
		}
		
		hiCut.process(left, right);
		cutFilter.process(left, right);
		
		if (chorus.isActive()) {
			chorus.processStereo(left, right);
		}
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
			delay.processAdd(left, left, true);
			delay.processAdd(right, right, false);
		}
		if (reverb.isActive()) {
			reverb.process(left, right);
		}
	}

    
}
