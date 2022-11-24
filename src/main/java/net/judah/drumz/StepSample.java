package net.judah.drumz;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.samples.Sample;

public class StepSample extends Sample {

	@Setter @Getter boolean on;
	int[] steps;
	
	public StepSample(String wavName, int... steps) throws Exception {
		super(JudahZone.getOutL(), JudahZone.getOutR(), wavName, Type.ONE_SHOT);
		this.steps = steps;
	}
	
	public void step(int step) {
		if (!on) return;
		if (step < 2 || step > 13 || step == 8)
			gain.setGain(0.95f); 
		else { // variation
			gain.setGain(0.5f + (step - 2 ) * (0.5f / 12f));
		}
		
		for (int x : steps)
			if (x == step) {
				setTapeCounter(0);
				active = true;
				return;
			}
	}

}
