package net.judah.synth;

import static net.judah.synth.SynthDB.DASH;

import lombok.Getter;
import net.judah.JudahZone;

@Getter
public class SynthPresets {

	private String current;
	private final JudahSynth synth;
	private final Adsr adsr;

	public SynthPresets(JudahSynth synth) {
		this.synth = synth;
		this.adsr = synth.getAdsr();
	}
	
	public void load(String name) {
		current = name;
		JudahZone.getSynthPresets().apply(name, this);
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
		synth.getGain().setGain(Float.parseFloat(dat[0]));
		synth.getHiCut().setFrequency(Float.parseFloat(dat[1]));
		synth.getHiCut().setResonance(Float.parseFloat(dat[2]));
		synth.getLoCut().setFrequency(Float.parseFloat(dat[3]));
		synth.getLoCut().setResonance(Float.parseFloat(dat[4]));
	}
	public void dco(String name, String[] dat) {
		int idx = Integer.parseInt(name.split(DASH)[1]);
		synth.getShapes()[idx] = Shape.valueOf(dat[0]);
		synth.getDcoGain()[idx] = Float.parseFloat(dat[1]);
		synth.getDetune()[idx] = dat.length > 2 ?
			synth.getDetune()[idx] = Float.parseFloat(dat[2]) : 1f;
	}

	public void update() {
		// TODO
	}


}
