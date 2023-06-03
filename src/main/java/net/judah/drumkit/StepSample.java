package net.judah.drumkit;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;

public class StepSample extends Sample {
	private static final float STEP_BOOST = 0.125f;
	
	@Setter @Getter boolean on;
	int[] steps;
	
	public StepSample(String wavName, Sampler sampler, int... steps) throws Exception {
		super(JudahZone.getOutL(), JudahZone.getOutR(), wavName, Type.ONE_SHOT, sampler);
		this.steps = steps;
		env = BOOST;
	}
	
	public void step(int step) {
		if (!on) return;
		if (step < 2 || step > 14 || step == 8)
			gain.setGain(0.85f); 
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
	
	@Override
	public void process() {
		if (!active || !hasRecording()) return;
		readRecordedBuffer();
		env = STEP_BOOST * sampler.stepMix;
		playFrame(leftPort.getFloatBuffer(), rightPort.getFloatBuffer()); 
    }


}
