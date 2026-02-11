package net.judah.drums.oldschool;

import static judahzone.util.Constants.LEFT;
import static judahzone.util.Constants.RIGHT;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import judahzone.api.AtkDec;
import judahzone.api.PlayAudio;
import judahzone.api.Played;
import judahzone.data.Asset;
import judahzone.data.Recording;
import judahzone.fx.Gain;
import judahzone.util.AudioTools;
import judahzone.util.Constants;
import lombok.Getter;
import lombok.Setter;
import net.judah.drums.DrumType;
import net.judah.midi.Actives;

/** Drum sample player with frame\-based attack/decay envelope. */
@Getter
public class DrumSample implements PlayAudio, AtkDec {

    public static final int MAX_ATTACK = 200; // milliseconds

    protected final float[] left = new float[Constants.bufSize()];
    protected final float[] right = new float[Constants.bufSize()];

    private final DrumType drumType;
    private final Gain gain = new Gain();
    private final Actives actives;
    @Setter protected float velocity = 1f;
    @Setter protected boolean onMute;
    @Setter protected Played played; // not used

    protected File file;
    protected boolean playing;
    protected Recording recording = new Recording();
    protected final AtomicInteger tapeCounter = new AtomicInteger(0);
    @Setter protected Type type = Type.ONE_SHOT;

    protected float[][] playBuffer;
    @Setter protected float env = 1f; // last-sample envelope/boost

    /** user parameters 0..100 */
    protected int attack = 1;
    protected int decay = 100;

    /* RT counters (per-sample), only mutated on the audio thread */
    private int atkSamples; // samples progressed in attack
    private int dkSamples;  // samples remaining in decay

    /* computed targets (frames), updated by setters / when playback starts */
    private volatile int attackFramesTarget = 0;
    private volatile int decayFramesTarget = 0;
    /* computed targets (samples) for RT use */
    private volatile int attackSamplesTarget = 0;
    private volatile int decaySamplesTarget = 0;

    public DrumSample(DrumType type, Actives actives) {
        this.drumType = type;
        this.actives = actives;
        // compute initial frames and sample targets with empty recording
        recomputeAttackFrames();
        recomputeDecayFrames();
    }

    @Override public void setAttack(int attack) {
        this.attack = Math.max(0, Math.min(100, attack));
        recomputeAttackFrames(); // updates frames *and* sample targets (non-RT)
        // do not touch RT counters here
    }

    @Override public void setDecay(int decay) {
        this.decay = Math.max(0, Math.min(100, decay));
        recomputeDecayFrames(); // non-RT
        // if playing, we'll recompute decay based on recording length in play(true)
    }

    /** set recording from Asset. */
    @Override public void setRecording(Asset asset) {
        if (asset == null) {
            setRecording((Recording) null);
            return;
        }
        this.file = asset.file();
        setRecording(asset.recording());
    }

    /** set recording directly. */
    protected void setRecording(Recording sample) {
        rewind();
        recording = sample == null ? new Recording() : sample;
        // recording length changed; recompute decay frames & sample target (non-RT)
        recomputeDecayFrames();
    }

    public String stripName(File f) {
        String n = f.getName();
        int idx = n.lastIndexOf('.');
        return idx > 0 ? n.substring(0, idx) : n;
    }

    @Override public void rewind() {
        tapeCounter.set(0);
    }

    @Override public void play(boolean play) {
        // when starting playback compute frame/sample targets dependent on recording length
        if (play && !this.playing) {
            // ensure attackFramesTarget is up to date
            recomputeAttackFrames();
            // reset RT counters
            atkSamples = 0;
            this.playing = true;
            // decay depends on recording length (remaining frames after attack)
            decayFramesTarget = computeDecayFrames(decay, recording.size(), attackFramesTarget);
            // compute sample targets (non-RT) for audio thread use
            int buf = Constants.bufSize();
            attackSamplesTarget = attackFramesTarget * buf;
            decaySamplesTarget = decayFramesTarget * buf;
            dkSamples = decaySamplesTarget; // start decay counter (samples remaining)
        } else if (!play) {
            this.playing = false;
        } else {
            this.playing = play;
        }
    }

    public void reset() {
        tapeCounter.set(0);
        playing = false;
        atkSamples = 0;
        dkSamples = decaySamplesTarget; // pre-computed (samples)
    }

    public void off() {
        playing = false;
        actives.removeData1(drumType.getData1());
    }

    protected void readRecordedBuffer() {
        int frame = tapeCounter.getAndIncrement();
        if (frame + 1 >= recording.size()) {
            tapeCounter.set(0);
            off();
        }
        playBuffer = recording.get(frame);
    }

    @Override public int getLength() {
        return recording.size();
    }

    @Override public final float seconds() {
        return getLength() / Constants.fps();
    }

    public void clear() {
        setRecording((Recording) null);
        playing = false;
        file = null;
    }

    @Override public final void setSample(long sample) {
        if (recording == null || recording.size() == 0 || sample <= 0) {
            tapeCounter.set(0);
            return;
        }

        int frames = recording.size();
        int samples = frames * Constants.bufSize();
        if (sample > samples)
            sample = samples - 1;
        int frame = (int) ((sample / (float) samples) * frames);
        tapeCounter.set(frame);
    }

    public int getVol() {
        return gain.get(Gain.VOLUME);
    }
    public int getPan() {
        return gain.get(Gain.PAN);
    }

    public void process(float[] outLeft, float[] outRight) {
        if (!playing) return;
        readRecordedBuffer();
        if (onMute)
            return;

        // Inline per-sample envelope application to avoid per-buffer scalar-only behavior.
        float[] srcL = playBuffer[LEFT];
        float[] srcR = playBuffer[RIGHT];
        float[] dstL = left;
        float[] dstR = right;

        int buf = Constants.bufSize();
        // precompute static multipliers for this buffer (no allocations)
        float gLeft = gain.getLeft() * gain.getGain();
        float gRight = gain.getRight() * gain.getGain();
        float vel = velocity;

        for (int i = 0; i < buf; i++) {
            float envSample = envelopeSample(); // per-sample envelope [0..1]
            env = envSample; // store last-sample envelope
            float scaleL = envSample * vel * gLeft;
            float scaleR = envSample * vel * gRight;
            dstL[i] = srcL[i] * scaleL;
            dstR[i] = srcR[i] * scaleR;
        }

        AudioTools.mix(left, outLeft);
        AudioTools.mix(right, outRight);
    }

    /* linear per-sample envelope: attack up, then decay down.
       No allocations, simple integer counters for RT safety. */
    private float envelopeSample() {
        // Attack stage: ramp from 0 -> 1 over attackSamplesTarget samples
        if (attackSamplesTarget > 0 && atkSamples < attackSamplesTarget) {
            atkSamples++;
            return atkSamples / (float) attackSamplesTarget;
        }
        // After attack: decay stage. If no decay samples, immediate silence.
        if (decaySamplesTarget <= 0)
            return 0f;
        // if dkSamples > 0 return dkSamples/decaySamplesTarget then decrement
        if (dkSamples > 0) {
            float v = dkSamples / (float) decaySamplesTarget;
            dkSamples--;
            return v;
        }
        return 0f;
    }

    private void recomputeAttackFrames() {
        // attack is percentage 0..100 mapped to 0..MAX_ATTACK milliseconds
        // convert ms->frames: frames = ms * framesPerSecond / 1000
        float ms = (attack / 100f) * MAX_ATTACK;
        int frames = Math.round(ms * Constants.fps() / 1000f);
        if (attack > 0 && frames == 0)
            frames = 1; // ensure at least one frame if non-zero
        attackFramesTarget = Math.max(0, frames);
        // non-RT compute samples target (safe here)
        attackSamplesTarget = attackFramesTarget * Constants.bufSize();
    }

    private void recomputeDecayFrames() {
        // decay depends on recording length; compute with current recording size
        decayFramesTarget = computeDecayFrames(decay, recording.size(), attackFramesTarget);
        // non-RT compute samples target (safe here)
        decaySamplesTarget = decayFramesTarget * Constants.bufSize();
    }

    private int computeDecayFrames(int decayPct, int recordingFrames, int attackFrames) {
        if (recordingFrames <= 0 || decayPct <= 0) return 0;
        int remaining = Math.max(0, recordingFrames - attackFrames);
        if (remaining == 0)
            return 0;
        int decayFrames = Math.round((decayPct / 100f) * remaining);
        if (decayPct > 0 && decayFrames == 0)
            decayFrames = 1;
        return decayFrames;
    }
}
