package net.judah.sampler;

import java.io.File;

import judahzone.util.Folders;
import lombok.Getter;
import lombok.Setter;

public class StepSample extends Sample {
	private static final float STEP_BOOST = 0.125f;

	@Setter @Getter private boolean on;
	private final int[] steps;

	public StepSample(String wavName, Sampler sampler, int... steps) throws Exception {
		super(wavName, new File(Folders.getSamples(), wavName + ".wav"), Type.ONE_SHOT, 0.5f);
		this.steps = steps;
		env = STEP_BOOST;
	}

	@Override public void play(boolean onOrOff) {
		playing = onOrOff;
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


}
