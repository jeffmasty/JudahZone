package net.judah.sampler;

import judahzone.data.Asset;
import lombok.Getter;
import lombok.Setter;

/**
 * StepSample now constructed from an Asset.
 */
public class StepSample extends Sample {
//    private static final float STEP_BOOST = 0.125f;

    @Setter @Getter private boolean on;
    private final int[] steps;

    public StepSample(Asset asset, Sampler sampler, int... steps) throws Exception {
        // Use asset file/name. Keeps same envelope and type as before.
        super(asset, Type.ONE_SHOT);  // DEFAULT_VOLUME
        this.steps = steps;
  //      env = STEP_BOOST;
    }

    @Override public void play(boolean onOrOff) {
        playing = onOrOff;
    }

    public void step(int step) {
        if (!on) return;
        if (step < 2 || step > 14 || step == 8)
            gain.setGain(0.85f);
        else { // variation
            gain.setGain((0.5f + (step - 2) * (0.5f / 12f)));
        }

        for (int x : steps)
            if (x == step) {
                rewind();
                playing = true;
                return;
            }
    }
}
