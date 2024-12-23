package net.judah.fx;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;
import java.util.Arrays;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.judah.omni.WavConstants;

public class Chorus implements TimeEffect {

    public enum Settings {
        Rate, Depth, Feedback, Type, Sync
    }

    private static final float PI2 = (float) Math.PI * 2;
    private static final float defaultRate = 3f;
    private static final float defaultDepth = 0.4f;
    private static final float defaultFeedback = 0.4f;

    private final int sampleRate;
    private final int nframes;

    @Getter @Setter boolean active;

    /** times per second */
    @Getter @Setter private float rate = defaultRate;
    /** between 0 and 1 */
    @Getter private float depth = defaultDepth;
    /** between 0 and 1 */
    @Getter private float feedback = defaultFeedback;
    /** between 0 and 1 */
    @Getter @Setter private float phase = 0.42f;

    private final LFODelay leftDsp;
    private final LFODelay rightDsp;
    private int lfocount;
	@Setter @Getter String type = TYPE[0];
	@Setter @Getter boolean sync;

    public Chorus(int sampleRate, int nframes) {
        this.sampleRate = sampleRate;
        this.nframes = nframes;

        leftDsp = new LFODelay();
        rightDsp = new LFODelay();

        setDepth(defaultDepth);
    }

    public Chorus() {
        this(WavConstants.S_RATE, WavConstants.JACK_BUFFER);
    }

    @Override public String getName() {
        return Chorus.class.getSimpleName();
    }

    @Override
    public int getParamCount() {
        return Settings.values().length;
    }

    @Override
    public int get(int idx) {
        if (idx == Settings.Rate.ordinal())
            return Math.round(getRate() * 10);
        if (idx == Settings.Depth.ordinal())
            return Math.round(getDepth() * 100);
        if (idx == Settings.Feedback.ordinal())
            return Math.round(getFeedback() * 100);
        if (idx == Settings.Type.ordinal())
        	return TimeEffect.indexOf(type);
        if (idx == Settings.Sync.ordinal())
        	return sync ? 1 : 0;
        throw new InvalidParameterException();
    }

    @Override
    public void set(int idx, int value) {
        if (idx == Settings.Rate.ordinal())
        	rate = value/10f;
        else if (idx == Settings.Depth.ordinal())
            setDepth(value/100f);
        else if (idx == Settings.Feedback.ordinal())
            feedback = value/100f;
        else if (idx == Settings.Type.ordinal() && value < TimeEffect.TYPE.length)
        	type = TimeEffect.TYPE[value];
        else if (idx == Settings.Sync.ordinal())
        	sync = value > 0;
        else throw new InvalidParameterException();
    }

    public void setDepth(float depth) {
        leftDsp.setDelay(depth / 1000);
        rightDsp.setDelay(depth / 1000);
        this.depth = depth;
    }

    public void processMono(FloatBuffer mono) {
        leftDsp.processReplace(mono);
    }

    public void processStereo(FloatBuffer left, FloatBuffer right) {
        leftDsp.processReplace(left);
        rightDsp.processReplace(right);
    }

    @Data
    class LFODelay {
        float range = 0.5f;
        float delay;
        float delayTime;
        float[] workArea;
        int rovepos = 0;
        float lastdelay;

        LFODelay() {
            workArea = new float[nframes];
            rovepos = 0;
            lastdelay = 0;
            reset();
        }

        void reset() {
            Arrays.fill(workArea, 0);
        }

        void goFigure() {
            if (rate > 0.01 && range > 0) {
                lfocount += nframes;
                float lfolength = sampleRate / rate;
                lfocount %= (int) (lfolength);
                float r = lfocount / lfolength;
                r *= PI2;
                r += phase * PI2;
                if (r > PI2) {
                    r -= PI2;
                }
                r = delay * range * (float) Math.sin(r);
                delayTime = delay + r;
            } else {
                lfocount = 0;
                delayTime = delay;
            }
        }

        void processReplace(FloatBuffer buf) {

            goFigure();
            buf.rewind();
            float delay = delayTime * sampleRate;
            float ldelay = lastdelay;

            int rnlen = workArea.length;
            int pos = rovepos;
            float delta = (delay - ldelay) / nframes;

            float r, s, a, b, o;
            int ri;
                for (int i = 0; i < nframes; i++) {
                    r = pos - (ldelay + 2) + rnlen;
                    ri = (int) r;
                    s = r - ri;
                    a = workArea[ri % rnlen];
                    b = workArea[(ri + 1) % rnlen];
                    o = a * (1 - s) + b * s;
                    workArea[pos] = buf.get(i) + o * feedback;
                    buf.put(o);
                    pos = (pos + 1) % rnlen;
                    ldelay += delta;
                }

            rovepos = pos;
            lastdelay = delay;
        }

        public void processAdd(FloatBuffer buf) {

            goFigure();
            buf.rewind();
            float delay = delayTime * sampleRate;
            float ldelay = lastdelay;

            int rnlen = workArea.length;
            int pos = rovepos;
            float delta = (delay - ldelay) / nframes;

            float r, s, a, b, o;
            int ri;
                for (int i = 0; i < nframes; i++) {
                    r = pos - (ldelay + 2) + rnlen;
                    ri = (int) r;
                    s = r - ri;
                    a = workArea[ri % rnlen];
                    b = workArea[(ri + 1) % rnlen];
                    o = a * (1 - s) + b * s;
                    workArea[pos] = buf.get(i) + o * feedback;
                    buf.put(buf.get(i) + o);
                    pos = (pos + 1) % rnlen;
                    ldelay += delta;
                }
            rovepos = pos;
            lastdelay = delay;
        }
    }



    @Override
	public void sync() {
    	sync(TimeEffect.unit());
    }

	@Override
	public void sync(float unit) {
		float msec = 0.001f * (unit + unit * TimeEffect.indexOf(type));
		setRate(msec);
	}

}

