package net.judah.drums.noise;

import judahzone.api.Key;
import judahzone.api.Note;
import judahzone.api.RTNotes;
import judahzone.fx.op.Noise;
import judahzone.fx.op.Noise.Colour;
import lombok.Getter;
import net.judah.drums.Drum;
import net.judah.drums.DrumSetup;
import net.judah.drums.DrumType;

/** Abstract drum oscillator.
  	Hybrid noise+resonator procedural percussive synthesis.
 	RT-safe w/ noteOff hook to UI. */
public abstract class NoiseOsc extends Drum {

	protected static final float  BLEND_SEC = 0.009f; // seconds over which to blend on retrigger
	protected static final int OVER_BLEND = Math.round(BLEND_SEC * OverSampled.SR_OVER); // frames over which to blend on retrigger
	protected static final int PHASE_BLEND = Math.round(BLEND_SEC * SR);

	Note midpoint = new Note(Key.A, 5); // 880

	protected volatile boolean dirty = true;

	@Getter protected final Noise noiseGen;

	protected float tonal = 1f;
	protected float atonal = 1f;

	public NoiseOsc(DrumType type, DrumSetup s, Colour colour, RTNotes actives) {
		super(s, actives);
	    noiseGen = new Noise(factor, colour);
	    setRoom((int)s.env().relMS()); // TODO unit?
	}

	protected void params() {
		for (int i = 0; i < 3; i++) {
		    set(i, get(i));
		}
	}

	protected abstract void generate();

	public void play(boolean play) {
	    this.open = play;
	    if (play)
	        env.trigger();
	}

//	@Override
//	public void offOff() {
//		super.offOff();
//		Threads.execute(() -> {
//			AudioTools.silence(work);
//			AudioTools.silence(left);
//			AudioTools.silence(right);
//		});
//	}

	// latest: fix idle/tail path to operate at base rate for oversampled units
	// TODO separate into super.process() filters envelope gain  (for all types: samples, noise, fm, etc)
	@Override
	public void processImpl() {
	    if (!isPlaying())
	        return;
	    generate();  // subclass work
	}
//
//	@Override
//	protected void post() {
//        // Tail processing must operate at *base* rate.
//        // If we are oversampled, decimate the work buffer first (in-place),
//        // then run delay / stereo / all-pass as in the active path.
////        if (this instanceof OverSampled)
////            AudioTools.decimate(work, work, OverSampled.FACTOR);
//
////        if (room > 1)
////            delay.process(left, right);
//
//        // gain.monoToStereo(work, left, right);
//
//        if (allPass != null) {
//            for (Schroeder ap : allPass[0]) ap.process(left);
//            for (Schroeder ap : allPass[1]) ap.process(right);
//        }
//	}
//
}