package net.judah.sampler;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;

public class StepSample extends Sample {
	private static final float STEP_BOOST = 0.125f;

	@Setter @Getter private boolean on;
	private final int[] steps;

	public StepSample(String wavName, Sampler sampler, int... steps) throws Exception {
		super(JudahZone.getOutL(), JudahZone.getOutR(), wavName, Type.ONE_SHOT, sampler);
		this.steps = steps;
		env = STEP_BOOST;
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
				rewind();
				playing = true;
				return;
			}
	}

	@Override
	public void process() {
		if (!playing) return;
		env = STEP_BOOST * sampler.stepMix;
		readSampleBuffer();
		playFrame(leftPort.getFloatBuffer(), rightPort.getFloatBuffer());
    }


}
