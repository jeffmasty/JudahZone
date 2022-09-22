package net.judah.mixer;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.MainFrame;
import net.judah.effects.Freeverb;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.GuitarTuner;

@Getter
public abstract class LineIn extends Channel {
    protected boolean muteRecord;
    @Setter protected boolean solo;
    protected LatchEfx latchEfx = new LatchEfx(this);
    /** set to <code>null</code> for no processing */
    @Setter protected GuitarTuner tuner;
    @Setter protected JackPort sync;
    
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
		if (reverb.isActive() && reverb.isInternal()) {
			reverb.process(mono);
		}
		if (cutFilter.isActive()) {
			cutFilter.process(mono);
		}

	}
	
	public void processFx(FloatBuffer left, FloatBuffer right) {
		left.rewind();
		right.rewind();
		
		if (this == GuitarTuner.getChannel()) {
			MainFrame.update(AudioTools.copy(left));
			left.rewind();
		}
		
		float gain = getVolume() * 0.5f;
		for (int z = 0; z < Constants.bufSize(); z++) {
			left.put(left.get(z) * gain);
			right.put(right.get(z) * gain);
		}

		if (eq.isActive()) {
			eq.process(left, true);
			eq.process(right, false);
		}
		if (chorus.isActive()) {
			chorus.processStereo(left, right);
		}
		if (overdrive.isActive()) {
			overdrive.processAdd(left);
			overdrive.processAdd(right);
		}

		if (delay.isActive()) {
			delay.processAdd(left, left, true);
			delay.processAdd(right, right, false);
		}
		if (reverb.isActive() && reverb.isInternal()) {
			((Freeverb)reverb).process(left, right);
		}
		if (cutFilter.isActive()) {
			cutFilter.process(left, right, 1);
		}

	}

    
}
