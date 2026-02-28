package net.judah.drums;

import javax.sound.midi.ShortMessage;

import judahzone.api.Frequency;
import judahzone.api.RTNotes;
import judahzone.filter.Coord;
import judahzone.filter.FilterT;
import judahzone.filter.MonoPeaking;
import judahzone.fx.Delay;
import judahzone.fx.EQ.EqBand;
import judahzone.fx.Gain;
import judahzone.fx.MonoFilter;
import judahzone.fx.MonoFilter.Type;
import judahzone.fx.op.Schroeder;
import judahzone.prism.Envelope;
import judahzone.prism.PrismUI;
import judahzone.util.AudioTools;
import judahzone.util.Constants;
import lombok.Getter;
import net.judah.drums.Drama.Freqs;

/** Base class for drum generators/oscillator-like units (Noise, Pluck, FM).
 	Holds common controls and a UI hook for custom knob mapping.
 	Subclasses pass in DrumParams.Settings  -> [gain envelope filters pitch]
  */
public abstract class Drum {

	public static interface NonPitched {} // marker for UI to hide pitch controls

	protected static final int SR = Constants.sampleRate();
	protected static final float ISR = 1.0f / SR;
	protected static final int N_FRAMES = Constants.bufSize();
	protected static final float TWO_PI = 2.0f * (float)Math.PI;
	protected static final int MAX_DECAY = 2048; // ms
	protected static final int MAX_ATTACK = 150; // ms
	protected static final float GATE = 0.083f; // short PT2399-type delay

	protected static final float RELEASE_TAIL = 0.8f; // seconds of max echos
	protected static final int RELEASE_SAMPLES = Math.round(RELEASE_TAIL * SR);
	private static final float MAX_DELAY_FEEDBACK = 0.4f; // safe max feedback (≈0.333)


	public static interface OverSampled {
	    int FACTOR = 2;
	    int SR_OVER = SR * FACTOR;
	    float ISR_OVER = ISR / FACTOR;
	}

    @Getter protected final Drama drama; // editor
	@Getter protected final DrumType type;
	@Getter protected final RTNotes actives;
	@Getter protected final Gain gain = new Gain();
	@Getter protected final MonoFilter lowCut;
	@Getter protected final MonoFilter hiCut;
	@Getter protected final MonoPeaking mid;

	/** two-stage envelope for drums applied after decimation */
	@Getter protected final Envelope env;
 	private final Delay delay = new Delay(GATE, true); // tail()

	protected volatile boolean open = false; // envelope open
	protected volatile boolean tail = false; // after envelope closes until RELEASE_SAMPLES have passed
	protected int room = 0;
	private int releaseSamples;
	protected Schroeder[][] allPass;

	/** pitch in hz */
	@Getter protected float fundamental;
	/** fundamental follows peak/mid filter */
	@Getter protected boolean sync; // TODO

    protected float velocity = 1f;
	/** oversample */
	protected final int factor;

	protected final float[] work;
	protected final float[] left = new float[N_FRAMES];
	protected final float[] right = new float[N_FRAMES];
    protected volatile boolean dirty = true;

	public Drum(DrumSetup s, RTNotes feedback) {
		type = s.t();
		actives = feedback;

		env = new Envelope(s.env());
	    factor = this instanceof OverSampled ? OverSampled.FACTOR : 1;
	    lowCut = new MonoFilter(Type.LoCut, s.freqs().lowCut().hz(), s.freqs().lowCut().reso(), 1);
	    mid = new MonoPeaking(new FilterT(s.freqs().body().hz(), s.freqs().body().dB(), 0.5f));
	    hiCut = new MonoFilter(Type.HiCut, s.freqs().hiCut().hz(), s.freqs().hiCut().reso(), factor);
	    gain.setPreamp(s.gainStage().preamp());
	    gain.setPan(s.gainStage().pan());
	    drama = new Drama(this, s.names());
		if (s.fundamental() != null)
			fundamental = s.fundamental().freq();

	    int factoredSize = N_FRAMES * factor;
	    work = new float[factoredSize];
	}


	/** fully over-write work buffer */
	protected abstract void processImpl();
	protected void post() {} // subclass allpass/schroeder

	/** Per-buffer chain: Generation -> Anti-alias -> Decimation -> EQ -> Envelope -> Panning. */
	public final void process(float[] sumL, float[] sumR) {
		if (!tail)
			return;
		if (!open) {
			tail();
			return;
		}

		processImpl(); // generate

		// 1. Anti-alias filter (at oversampled rate if applicable)
		hiCut.process(work);

		// 2. Downsample to system rate
		if (factor > 1)
			AudioTools.decimate(work, work, factor);

		// 3. System-rate EQ (correct order for drum 'tone' shaping)
		lowCut.process(work);
		if (mid.getGainDb() != 0f)
			mid.process(work);

		// 4. Amplitude Envelope (Final gate to ensure absolute silence)
		open = env.process(work) == N_FRAMES;

		// mono delay, if any
		if (room > 1)
			delay.process(work);
		// 5. Spatialization
		gain.monoToStereo(work, left, right);

		// 6. Subclass FX (Delays, Resonators inside post())
		post();

		// 7. Accummulate to main mix
		AudioTools.mix(left, velocity, sumL);
		AudioTools.mix(right, velocity, sumR);
	}

	private void tail() { // !open
		if (!tail)
			return;

		AudioTools.silence(work);
		if (room > 1)
			delay.process(work);
		gain.monoToStereo(work, left, right);
		post();

		releaseSamples += N_FRAMES;
		if (releaseSamples > RELEASE_SAMPLES) {
			tail = false;
			releaseSamples = 0;
			delay.reset();
		}
	}

	public final void off() {
		env.release();
	    open = false;
	    actives.removeData1(type.getData1());
	}

	public void offOff() {
		if (open)
			off();
		tail = false;
	}

	public final boolean isPlaying() {
		return open;
	}

	public final boolean isSounding() {
		return tail || open;
	}
	public void setHz(EqBand type, float hz) {
	    switch(type) {
	        case Bass -> lowCut.setFrequency(hz);
	        case High -> hiCut.setFrequency(hz);
	        case Mid -> {
	            mid.setFrequency(hz);
	            dirty = true;
	        }
	    }
	}

	public void setResonance(EqBand type, float db) {
	    switch(type) {
	        case Bass -> lowCut.setResonance(db);
	        case High -> hiCut.setResonance(db);
	        case Mid -> mid.setGainDb(db);
	    }
	}

	/** Ranged between current lo and hi cut. */
	public int getPitchKnob() {
	    return getHzKnob(EqBand.Mid);
	}

	public int getHzKnob(EqBand type) {
	    float hz = switch(type) {
	        case Bass -> lowCut.getFrequency();
	        case High -> hiCut.getFrequency();
	        case Mid -> mid.getFrequency();
	    };
	    return Constants.reverseLog(hz, Frequency.MIN, Frequency.MAX);
	}

	public int getResonanceKnob(EqBand type) {
	    return switch(type) {
	        case Bass -> (int) lowCut.getResonance() * 4;
	        case High -> (int) hiCut.getResonance() * 4;
	        case Mid -> (int) ((mid.getGainDb() + 24.0f) / 48.0f * 100.0f); // maps -24..+24 dB to 0..100
	    };
	}

	public void setResonanceKnob(EqBand type, int knob) {
		if (type == EqBand.Mid) {
			// knob 0..100 maps to +/- 24.0dB
			float db = (knob / 100.0f) * 48.0f - 24.0f;
			mid.setGainDb(db);
		}
		else {
		    float db = (knob / 100f) * 24f;
		    setResonance(type, db);
		}
	}

	public void setHzKnob(EqBand type, int knob) {
	    float hz = Constants.logarithmic(knob, Frequency.MIN, Frequency.MAX);
	    setHz(type, hz);
	}

	public float getHz(EqBand type) {
	    return switch(type) {
	        case Bass -> lowCut.getFrequency();
	        case High -> hiCut.getFrequency();
	        case Mid -> mid.getFrequency();
	    };
	}

	public float getResonance(EqBand type) {
	    return switch(type) {
	        case Bass -> lowCut.getResonance();
	        case High -> hiCut.getResonance();
	        case Mid -> mid.getGainDb();
	    };
	}

	public Freqs getFreqs() {
	    return new Freqs(
	        new Coord(lowCut.getFrequency(), lowCut.getResonance()),
	        new FilterT(mid.getFrequency(), mid.getGainDb(), mid.getBandwidth()),
	        new Coord(hiCut.getFrequency(), hiCut.getResonance()));
	}

	/** Trigger note: setup velocity, delegate retrigger ramp to env, and reset counters. */
	public void trigger(ShortMessage midi) {
		releaseSamples = 0;
	    velocity = midi == null ? 100 : Constants.midiToFloat(midi.getData2());
	    env.trigger(); // Letter defined smooth (4-6ms)
	    open = true;
	    tail = true;
	    // frameCounter = 0;
	}

	/** Map room knob (0..100) to delay feedback (0.0..MAX_DELAY_FEEDBACK). */
	public final void setRoom(int fb) {
	    room = Math.max(0, Math.min(100, fb));
	    float scaled = room * 0.01f * MAX_DELAY_FEEDBACK;
	    delay.setFeedback(scaled);
	}
    /** Release the unit (UI side). */
    @PrismUI
    public final void release() {
    	env.release();
    }

	public void setFundamental(float fundamental) {
		this.fundamental = fundamental;
		dirty = true;
	}

	public final void setPitchKnob(byte knob) {
		setFundamental(Frequency.LUT[knob]);
	}

	public final void setSync(boolean sync) {
		this.sync = sync;
		// TODO implement tracking
	}


	/** Build stereo all-pass network from delay times (samples). */
	protected final void network(float fb, int... delays) {
	    Schroeder[][] result = new Schroeder[2][delays.length];
	    for (int i = 0; i < delays.length; i++) {
	        result[0][i] = new Schroeder(delays[i], fb);
	        result[1][i] = new Schroeder(Schroeder.decorrelate(delays[i], i), fb);
	    }
	    allPass = result;
	}

	/** Modulate all-pass feedback (scaled by 0.6 for stability). */
	protected final void allpass(float fb) {
	    float scaled = fb * 0.6f;
	    if (allPass != null)
	        for (int i = 0; i < allPass[0].length; i++) {
	            allPass[0][i].setFeedback(scaled);
	            allPass[1][i].setFeedback(scaled);
	        }
	}

	public abstract void set(int idx, int knob); // custom
	public abstract int get(int idx); // custom
	public String val(int idx) { // child override: bool/Colour/hz/note, etc
		return "" + get(idx);
	};


}
