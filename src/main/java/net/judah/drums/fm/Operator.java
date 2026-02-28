
package net.judah.drums.fm;

import java.util.function.Supplier;

import judahzone.data.Letter;
import judahzone.data.Shape;
import judahzone.prism.Envelope;
import judahzone.prism.PrismRT;
import judahzone.util.Constants;
import judahzone.util.Interpolation;
import judahzone.util.Phase;
import lombok.Getter;
import lombok.Setter;
import net.judah.drums.fm.FMSetup.OpSetup;

@PrismRT public final class Operator implements Supplier<OpSetup> {

    public static enum Params { Level, Ratio, Feedback, Attack, Decay, Shape }

    private static final int MASK = Shape.MASK;
    private static final int LEN = Shape.LENGTH;

    private final float isr;

    @Getter private final Envelope envelope = new Envelope(new Letter());
    @Getter private Shape shape;
    private Interpolation interpolator = Interpolation.CUBIC;
    @Getter private final FMRatio ratio;
    @Setter @Getter private float level = 1f;
    private final Phase phase = new Phase();
    private float phaseInc = 0f;

    @Setter private boolean selfFeedback = true;
    @Getter private float feedback = 0f;
    /** kept name for ABI convenience: now represents feedback * phase\-scale (radians). */
    float feedbackHz = 0f;
    float lastOut = 0f;

    public Operator(OpSetup setup, boolean carrier, float invSR) {
    	isr = invSR > 0 && invSR < 0.1 ? invSR : 1 / 48_000;
        level = setup.level();
        envelope.setAttackMs(setup.env().atkMS());
        envelope.setDecayMs(setup.env().dkMS());
        setFeedback(setup.fb());
        ratio = new FMRatio(setup.ratio());
        setShape(setup.shape());
        envelope.addPhase(phase);
    }

    @Override public OpSetup get() {
        return new OpSetup(level, feedback, ratio.get(), envelope.getPostage(), shape);
    }

    /**
     * Generate with DX-style phase modulation:
     * - incoming `externalMod` is interpreted as radians (modulatorOut * MOD_HZ_SCALE from FMOsc).
     * - advance the phase accumulator by `phaseInc` (cycles/sample).
     * - add modulation as a phase offset in cycles: radians * (1 / 2π).
     */
    public void generate(float[] output, int destination, float externalMod) {
        if (!envelope.isPlaying()) return;

        float env = envelope.process();
        // Terse check for silent envelope stages
        if (env <= 0.0001f && !envelope.isPlaying()) return;

        // feedbackHz now stores radians-per-unit-feedback (fb knob * MOD_HZ_SCALE)
        float fbModRad = selfFeedback ? feedbackHz * lastOut : 0f; // radians
        float modRad = externalMod + fbModRad; // radians total from modulators+feedback

        // Advance phase by the base increment (cycles/sample) and read blended phase.
        float basePhaseCycles = phase.next(phaseInc);

        // Convert modulation radians -> cycles and add as a phase offset (DX-style PM).
        float phaseVal = basePhaseCycles + modRad * Constants.I2PI;

        float idxf = phaseVal * LEN;
        int idx = ((int) idxf) & MASK;
        float frac = idxf - (int) idxf;

        float[] table = shape.getWave();
        // Cubic interpolation for spectral purity
        float sample = interpolator.interp(table[(idx - 1) & MASK], table[idx],
                table[(idx + 1) & MASK], table[(idx + 2) & MASK], frac);

        float out = sample * level * env;
        output[destination] += out;
        lastOut = out;
    }

    public void reset() {
        lastOut = 0f;
        phase.reset();
    }

    public void setFeedback(float fb) {
        this.feedback = fb;
        // interpret feedback knob as depth; store radians-scale for per-sample multiply.
        this.feedbackHz = fb * Constants.TWOPI;
    }

    public void setRatio(float theta) {
        ratio.set(theta);
    }

    public void setShape(Shape shape) {
//    	if (shape == null || shape == Shape.SQR)
//    		throw new InvalidParameterException("FM waves too much DC");
		this.shape = shape;
		interpolator = shape == Shape.SQR ? Interpolation.LINEAR : Interpolation.CUBIC;
	}


    /**
     * Compute and set the phase increment expressed in *cycles per sample*.
     * Use the operator ratio (linear multiplier) and the global sample rate to
     * produce a units-consistent increment for the Phase accumulator.
     */
    public void phaseInc(float hz) {
        // hz * ratio => frequency in Hz for this operator.
        // divide by sampleRate -> cycles per sample.
        this.phaseInc = (ratio.get() * hz) * isr;
    }

    public void trigger(float baseHz) {
        reset();
        // Compute cycles/sample using current ratio and sample rate (same unit as phaseInc()).
        phaseInc = (ratio.get() * baseHz) * isr;
        envelope.trigger();
    }

    public boolean isSelfFeedback() {
        return selfFeedback;
    }
}
