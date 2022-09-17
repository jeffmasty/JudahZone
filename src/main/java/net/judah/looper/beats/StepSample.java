package net.judah.looper.beats;

import net.judah.JudahZone;
import net.judah.looper.sampler.Sample;

public class StepSample extends Sample {

	public StepSample(String wavName) throws Exception {
		super(JudahZone.getLooper(), wavName, Type.ONE_SHOT);
	}
	
	// TODO
	public void step(int step) {
		twoAndFour(step);
	}
	
	private void twoAndFour(int step) {
		if (step == 4 || step == 12) {
			setTapeCounter(0);
			active = true;
		}
	}


}
