package net.judah.drumz;

import net.judah.JudahZone;
import net.judah.samples.Sample;

public class StepSample extends Sample {

	public StepSample(String wavName) throws Exception {
		super(JudahZone.getOutL(), JudahZone.getOutR(), wavName, Type.ONE_SHOT);
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
