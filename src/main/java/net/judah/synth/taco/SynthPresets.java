package net.judah.synth.taco;

import static net.judah.synth.taco.SynthDB.DASH;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.util.RTLogger;

@Getter
public class SynthPresets {

	private String current;
	private final TacoSynth synth;
	private final Adsr adsr;

	public SynthPresets(TacoSynth synth) {
		this.synth = synth;
		this.adsr = synth.getAdsr();
	}
	
	public boolean load(String name) {
		try {
			if (JudahZone.getSynthPresets().apply(name, this)) {
				current = name;
				return true;
			}
		} catch (Throwable t) { RTLogger.warn(this, t); }
		return false;
	}
	
	public int getProg() {
		return JudahZone.getSynthPresets().getProg(current);
	}
	
	public void adsr(String[] dat) {
		adsr.setAttackTime(Integer.parseInt(dat[0]));
		adsr.setDecayTime(Integer.parseInt(dat[1]));
		adsr.setSustainGain(Float.parseFloat(dat[2]));
		adsr.setReleaseTime(Integer.parseInt(dat[3]));
	}
	public void filter(String[] dat) {
		synth.getHiCut().setFrequency(Float.parseFloat(dat[0]));
		synth.getHiCut().setResonance(Float.parseFloat(dat[1]));
		synth.getLoCut().setFrequency(Float.parseFloat(dat[2]));
		synth.getLoCut().setResonance(Float.parseFloat(dat[3]));
	}
	public void dco(String name, String[] dat) {
		int idx = Integer.parseInt(name.split(DASH)[1]);
		synth.getShapes()[idx] = Shape.valueOf(dat[0]);
		synth.getDcoGain()[idx] = Float.parseFloat(dat[1]);
		synth.getDetune()[idx] = dat.length > 2 ?
			synth.getDetune()[idx] = Float.parseFloat(dat[2]) : 1f;
	}

}
