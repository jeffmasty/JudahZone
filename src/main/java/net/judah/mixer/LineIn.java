package net.judah.mixer;

import java.nio.FloatBuffer;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.GuitarTuner;
import net.judah.util.AudioTools;
import net.judah.util.Constants;

@Getter
public abstract class LineIn extends Channel {

	@Setter protected float factor = 4;
	protected boolean muteRecord;
    /** set to <code>null</code> for no processing */
    @Setter protected GuitarTuner tuner;
    
    public LineIn(String name, boolean stereo) {
    	super(name, stereo);
    }
    
    public void setMuteRecord(boolean muteRecord) {
		this.muteRecord = muteRecord;
		MainFrame.update(this);
	}

    public void toStereo(FloatBuffer work) {
		work.rewind();
		left.rewind();
		right.rewind();
		float l = gain.getLeft();
		float r = gain.getRight();
		for (int i = 0; i < work.capacity(); i++) {
			left.put(work.get(i) * l);
			right.put(work.get(i) * r);
		}
    }
    
	public void processFx(FloatBuffer mono) {
		mono.rewind();
		if (this == GuitarTuner.getChannel()){
			float[] mem = JudahZone.getLooper().getMemory().getArray()[0];
			AudioTools.copy(mono, bufSize, mem);
			MainFrame.update(mem);
		}
		for (int z = 0; z < Constants.bufSize(); z++)
			mono.put(mono.get(z) * factor);

		filter1.process(mono);
		filter2.process(mono);
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
			delay.process(mono, mono, true);
		}
		if (compression.isActive())
			compression.process(mono);
		if (reverb.isActive() && reverb.isInternal()) {
			reverb.process(mono);
		}
	}
	
	// stereo
	public void processStereoFx(float amplification) {
		left.rewind();
		right.rewind();
		
		if (this == GuitarTuner.getChannel()) {
			float[] mem = JudahZone.getLooper().getMemory().getArray()[0];
			AudioTools.copy(left, bufSize, mem);
			MainFrame.update(mem);
		}

		float l = amplification * (1 - gain.getStereo());
		float r = amplification * gain.getStereo();
		for (int z = 0; z < bufSize; z++) {
			left.put(left.get(z) * l);
			right.put(right.get(z) * r);
		}
		
		filter2.process(left, right);
		filter1.process(left, right);
		
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
			delay.process(left, left, true);
			delay.process(right, right, false);
		}
		if (reverb.isActive()) {
			reverb.process(left, right);
		}
	}

    
}
