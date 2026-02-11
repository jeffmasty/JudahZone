package net.judah.drums.synth;

import judahzone.api.AtkDec;
import judahzone.api.Key;
import judahzone.api.Note;
import judahzone.data.Filter;
import judahzone.data.Frequency;
import judahzone.fx.Delay;
import judahzone.fx.EQ.EqBand;
import judahzone.fx.Gain;
import judahzone.fx.MonoFilter;
import judahzone.fx.MonoFilter.Type;
import judahzone.fx.op.MonoPeaking;
import judahzone.fx.op.NoiseGen;
import judahzone.fx.op.Schroeder;
import judahzone.util.AudioTools;
import judahzone.util.Constants;
import judahzone.util.SineWave;
import lombok.Getter;
import net.judah.drums.DrumType;
import net.judah.drums.synth.DrumParams.DrumParam;
import net.judah.drums.synth.DrumParams.Freqs;
import net.judah.midi.Actives;

/** Abstract drum oscillator. RT-safe: defers note removal to GUI/MIDI thread. */
public abstract class DrumOsc implements AtkDec {

    protected static final int SR = Constants.sampleRate();
    protected static final float ISR = 1.0f / SR;
    protected static final int N_FRAMES = Constants.bufSize();
    protected static final float TWO_PI = 2.0f * (float)Math.PI;
    protected static final int MAX_DECAY = 1250; // ms
    protected static final int MAX_ATTACK = 150; // ms
    protected static final float GATE = 0.085f; // short default delay
    /* Seconds to defer off() so delay/reverb can ring. */
    protected static final float RELEASE_TAIL = 0.55f; // seconds of max echos
    protected static final int RELEASE_SAMPLES = Math.round(RELEASE_TAIL * SR);

    // temp midpoint
    Note midpoint = new Note(Key.A, 5); // 880

    public static interface OverSampled { // flag
        int FACTOR = 2;
        int SR_OVER = SR * FACTOR;     // oversampled sample rate
        float ISR_OVER = ISR / FACTOR; // oversampled inverse sample rate
    }
    protected static final SineWave lookup = new SineWave();

    @Getter protected final DrumType type;
    @Getter private final DrumParams.Settings settings;

    protected final Actives actives;

    protected boolean playing = false;
    protected volatile boolean dirty = true;
    protected int frameCounter = 0;
    private float velocity;

    protected final float[] work;
    protected final float[] left;
    protected final float[] right;

    @Getter protected final Gain gain = new Gain(); // mostly use midi velocity for gain
    @Getter protected int attack = 10; // 1..200 ms (user percentage style will map below)
    @Getter protected int decay = 100; // 1..500 ms (decay percent mapped to MAX_DECAY)

	protected final MonoPeaking pitch; // center frequency data, optional boost/cut
    @Getter protected final MonoFilter lowCut; // getter for rangeSlider
    @Getter protected final MonoFilter hiCut;  // getter for rangeSlider
    @Getter protected final NoiseGen noiseGen; // getter for Snare GUI

    protected float tonal = 1f; // multiplier for the pitched component of the sound (sine-based body)
    protected float atonal = 1f; // multiplier for the non-pitched component of the sound (noise, clicks, etc)

    protected final Delay delay = new Delay(GATE, true); // optional delay for cheap reverb
	protected Schroeder[][] allPass; // optional all-pass network for more complex reverb
    protected int room = 0;

    /* RT counters (samples) for envelope */
    private int atkSamples = 0;     // progressed in attack
    private int dkSamples = 0;      // remaining samples in decay

    protected volatile int atkTarget = 0;
    protected volatile int dkTarget = 0; // full decay length in samples (maps decay=100 -> MAX_DECAY)
    private float invAttack, invDecay;

    /* Smoothing for attack changes (to avoid clicks for very short attack times). */
    private volatile float invAttackTarget = 0f; // desired inverse-attack (1/atkTarget)
    private int attackSmoothCounter = 0; // remaining frames to smooth over
    private final int attackSmoothFramesDefault = Math.max(1, Math.round(0.005f * SR)); // ~5ms smoothing
    private volatile boolean attackChanged = false; // flag: attack param changed, need to smooth

    /* Retrigger ramp to avoid discontinuities. */
    private volatile boolean retrigActive = false;
    private int retrigRampSamples = 0;
    private int retrigRampCounter = 0;
    /* Retrigger phase blending to avoid discontinuities */
    protected float prevPhase = 0f;
    protected float prevClickPhase = 0f;  // for drums with click/tap
    protected volatile boolean retrigPhaseBlend = false;
    protected int phaseBlendSamples = 0;
    protected int phaseBlendCounter = 0;


    public DrumOsc(DrumType type, DrumParams.Settings s, Actives actives) {

        this.actives = actives;
        this.type = type;

        int factor = this instanceof OverSampled ? OverSampled.FACTOR : 1;
        int size = N_FRAMES * factor;
        work = new float[size];
        left = new float[size];
        right = new float[size];

        settings = s;
        noiseGen = new NoiseGen(factor, s.colour());
        attack = s.env().attack();
        decay = s.env().decay();
        setRoom(s.env().release());
        gain.setPreamp(s.gainStage().vol());
        gain.set(Gain.PAN, s.gainStage().pan());

        Freqs f = s.freqs();
        hiCut = new MonoFilter(Type.HiCut, f.hiCut().hz(), f.hiCut().reso(), factor);
        lowCut = new MonoFilter(Type.LoCut, f.lowCut().hz(), f.lowCut().reso(), 1);
        pitch = new MonoPeaking(f.body().hz(), f.body().reso());

        computeAttack();
        computeDecay();
    }

    public abstract void set(DrumParam idx, int knob);
    public abstract int get(DrumParam idx);
    protected abstract void generate();

    protected void network(float fb, int... delays) {
    	Schroeder[][] result = new Schroeder[2][delays.length];
    	for (int i = 0; i < delays.length; i++) {
    		int delay = delays[i];
    		result[0][i] = new Schroeder(delay, fb);
    		result[1][i] = new Schroeder(Schroeder.decorrelate(delay, i), fb);
		}
		allPass = result;
    }

    protected void allpass(float fb) {
    	float scaled = fb * 0.6f;
    	if (allPass != null) {
    		for (int i = 0; i < allPass[0].length; i++) {
    			allPass[0][i].setFeedback(scaled);
    			allPass[1][i].setFeedback(scaled);
    		}
    	}
    }

	/** Room is a knob 0..100. Map porprotionally to Delay feedback (0.0..0.333). */
	public void setRoom(int fb) {
		room = Math.max(0, Math.min(100, fb));
		float scaled = room * 0.01f * RELEASE_TAIL;
		delay.setFeedback(scaled);
	}

    @Override public void setAttack(int attack) {
        this.attack = Math.min(100, Math.max(1, attack));
        dirty = true;
        computeAttack();
    }

    @Override public void setDecay(int decay) {
        this.decay = Math.min(decay, MAX_DECAY);
        dirty = true;
        computeDecay();
    }

//////////////////////////////////////////////////////

    public void setHz(EqBand type, float hz) {
    	switch(type) {
			case Bass: lowCut.setFrequency(hz); break;
			case High: hiCut.setFrequency(hz); break;
			case Mid:
				pitch.setFrequency(hz);
				dirty = true;
				break;
    	}
    }

    public void setResonance(EqBand type, float db) {
		switch(type) {
			case Bass: lowCut.setResonance(db); break;
			case High: hiCut.setResonance(db); break;
			case Mid: pitch.setGainDb(db); break;
		}
	}


    /** ranged between current lo and hi cut */
    public int getPitchKnob() {
		return getHzKnob(EqBand.Mid);
	}

    public int getHzKnob(EqBand type) {
    	float hz = switch(type) {
    				case Bass -> lowCut.getFrequency();
    				case High -> hiCut.getFrequency();
    				case Mid -> pitch.getFrequency();
    	};
    	return Constants.reverseLog(hz, Frequency.MIN, Frequency.MAX);
	}

    public int getResonanceKnob(EqBand type) {
    	return switch(type) {
    		case Bass -> (int) lowCut.getResonance() * 4;
    		case High -> (int) hiCut.getResonance() * 4;
    		case Mid -> (int) pitch.getGainDb() * 4;
    	};
    }

    public void setHzKnob(EqBand type, int knob) {
		float hz = Constants.logarithmic(knob, Frequency.MIN, Frequency.MAX);
		setHz(type, hz);
	}

    public void setResonanceKnob(EqBand type, int knob) {
    	float db = (knob / 100f) * 24f;
    	setResonance(type, db);
    }

    public void setHz(float hz) {
    	setHz(EqBand.Mid, hz);
	}

    public float getHz() {
    	return getHz(EqBand.Mid);
    }

    public float getHz(EqBand type) {
		return switch(type) {
			case Bass -> lowCut.getFrequency();
			case High -> hiCut.getFrequency();
			case Mid -> pitch.getFrequency();
		};
    }

    public float getResonance(EqBand type) {
		return switch(type) {
			case Bass -> lowCut.getResonance();
			case High -> hiCut.getResonance();
			case Mid -> pitch.getGainDb();
		};
    }

    public Freqs getFreqs() {
		return new Freqs(new Filter(lowCut.getFrequency(), lowCut.getResonance()),
				new Filter(pitch.getFrequency(), pitch.getGainDb()),
				new Filter(hiCut.getFrequency(), hiCut.getResonance()));
	}

    public void setPitchKnob(int knob) {
    	setHzKnob(EqBand.Mid, knob);
    }


/////////////////////////////////////
    public void trigger(int data2) {
        velocity = Constants.midiToFloat(data2);
        // If retriggering while playing, set a short ramp to suppress
        // the discontinuity/click. Keep ramp length tiny and bounded by attack.
        int desiredRamp = Math.max(1, Math.round(0.005f * SR)); // ~5ms
        int boundedRamp = atkTarget > 0 ? Math.min(desiredRamp, atkTarget) : desiredRamp;
        retrigRampSamples = boundedRamp;
        retrigRampCounter = 0; // can we get the current envelop if retriggering?  0 is too discontinuous
        retrigActive = true;

        playing = true;
        frameCounter = 0;
        atkSamples = 0;
        dkSamples = dkTarget;
    }

    public void off() {
        playing = false;
        actives.removeData1(type.getData1());
    }


    public void play(boolean play) {
        this.playing = play;
        if (play) {
            // ensure targets are up-to-date when playback resumes
            computeAttack();
            computeDecay();
        }
    }

    public void process(float[] sumL, float[] sumR) {
        if (!playing) {
            AudioTools.silence(work);
            postEnvelope();
            return;
        }

        AudioTools.silence(work);
        generate();

        hiCut.process(work);

        if (this instanceof OverSampled)
            AudioTools.decimate(work, work, OverSampled.FACTOR);

        lowCut.process(work);
        if (pitch.getGainDb() != 0f)
        	pitch.process(work);

        envelope();
        postEnvelope();

        gain.monoToStereo(work, left, right);

        if (allPass != null) {
        	for (Schroeder ap : allPass[0])
				ap.process(left);
        	for (Schroeder ap : allPass[1])
        		ap.process(right);
        }

        AudioTools.mix(left, velocity, sumL);
        AudioTools.mix(right, velocity, sumR);

        // Defer off() to allow delay tail to ring naturally // TODO Gui doesn't want delay tails
        if (dkSamples <= 0) {
            frameCounter++;
            if (frameCounter > atkTarget + dkTarget + RELEASE_SAMPLES)
                off();
        }
    }

    protected void envelope() {
        // Apply per-sample envelope across the work buffer without allocations
        int buf = work.length; // may be N_FRAMES or oversampled size

        for (int i = 0; i < buf; i++) {

            // Attack stage
            if (atkTarget > 0 && atkSamples < atkTarget) {
                atkSamples++;
                // Smooth invAttack only when attack param changes (on first frame after change)
                if (attackChanged) {
                    attackSmoothCounter = attackSmoothFramesDefault;
                    attackChanged = false; // only smooth once per parameter change
                }
                // Step invAttack toward target if smoothing is active
                if (attackSmoothCounter > 0) {
                    float step = (invAttackTarget - invAttack) / attackSmoothCounter;
                    invAttack += step;
                    attackSmoothCounter--;
                }
                float env = atkSamples * invAttack;
                work[i] *= env;
                // decrement decay counter as samples pass
                if (dkSamples > 0) dkSamples--;
            } else {
                // decay stage
                if (dkTarget <= 0) {
                    work[i] = 0f;
                } else if (dkSamples > 0) {
                    float env = dkSamples * invDecay;
                    work[i] *= env;
                    dkSamples--;
                } else {
                    work[i] = 0f;
                }
            }

            // Apply a very short retrigger ramp (multiplicative) to suppress
            // initial discontinuity if we were just retriggered.
            if (retrigActive) {
                // safe division: retrigRampSamples >= 1
                float r = (retrigRampCounter >= retrigRampSamples) ? 1.0f
                        : (retrigRampCounter / (float) retrigRampSamples);
                work[i] *= r;
                retrigRampCounter++;
                if (retrigRampCounter >= retrigRampSamples)
                    retrigActive = false;
            }

            frameCounter++;
        }
    }

    private void computeAttack() {
        // Map attack percentage 0..100 -> 0..MAX_ATTACK ms, then to samples
        float ms = (attack / 100f) * MAX_ATTACK;
        int samples = Math.round(ms * SR / 1000f);
        if (attack > 0 && samples == 0) samples = 1; // always something
        // When target shrinks, clamp current progress to avoid skipping beyond new attack
        if (samples < atkTarget) {
            atkSamples = Math.min(atkSamples, samples);
        }
        atkTarget = Math.max(0, samples);
        // Set smooth target; signal that attack changed (flag will reset in envelope())
        invAttackTarget = atkTarget > 0 ? (1f / atkTarget) : 0f;
        attackChanged = true;
        // If invAttack not yet initialized, seed it to avoid division by zero
        if (invAttack == 0f) invAttack = invAttackTarget;
    }

    private void computeDecay() {
        // Map decay percentage 0..100 -> 0..MAX_DECAY ms, then to samples
        float ms = (decay / 100f) * MAX_DECAY;
        int samples = Math.round(ms * SR / 1000f);
        if (decay > 0 && samples == 0) samples = 1;
        dkTarget = Math.max(0, samples);
        invDecay = dkTarget > 0 ? 1f / dkTarget : 0f;
    }

    protected void postEnvelope() {
    	if (room > 1)
    		delay.process(work, null);
    }

}
