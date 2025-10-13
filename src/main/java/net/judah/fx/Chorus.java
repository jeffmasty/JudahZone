package net.judah.fx;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import lombok.Setter;

// JAudioLabs Source?
public class Chorus implements TimeEffect {

    public enum Settings {
        Rate, Depth, Feedback, Type, Sync
    }

    private static final float PI2 = (float) Math.PI * 2;
    private static final float defaultRate = 3f;
    private static final float defaultDepth = 0.4f;
    private static final float defaultFeedback = 0.4f;

    @Getter @Setter boolean active;
	@Setter @Getter boolean sync;
	@Setter @Getter String type = TYPE[0];

	/** times per second */
    @Getter @Setter private float rate = defaultRate;
    /** between 0 and 1 */
    @Getter private float depth = defaultDepth;
    /** between 0 and 1 */
    @Getter private float feedback = defaultFeedback;
    /** between 0 and 1 */
    @Getter @Setter private float phase = 0.42f;

    private final LFODelay leftDsp = new LFODelay();
    private final LFODelay rightDsp = new LFODelay();
    private int lfocount;

    @Override
	public void sync() {
    	sync(TimeEffect.unit());
    }

	@Override
	public void sync(float unit) {
		float msec = 0.001f * (unit + unit * TimeEffect.indexOf(type));
		setRate(msec);
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

    void setDepth(float depth) {
        leftDsp.setDelay(depth / 1000);
        rightDsp.setDelay(depth / 1000);
        this.depth = depth;
    }

    @Override
	public void process(FloatBuffer left, FloatBuffer right) {
        leftDsp.processReplace(left);
        rightDsp.processReplace(right);
    }

    private class LFODelay {

        @Setter float delay = depth * 0.001f;
        float[] workArea = new float[N_FRAMES];
        float range = 0.5f;
        float delayTime;
        int rovepos;
        float lastdelay;

        void goFigure() {
            if (rate > 0.01 && range > 0) {
                lfocount += N_FRAMES;
                float lfolength = SAMPLE_RATE / rate;
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
            float delay = delayTime * SAMPLE_RATE;
            float ldelay = lastdelay;

            int rnlen = workArea.length;
            int pos = rovepos;
            float delta = (delay - ldelay) / N_FRAMES;

            float r, s, a, b, o;
            int ri;
                for (int i = 0; i < N_FRAMES; i++) {
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

        @SuppressWarnings("unused")
		public void processAdd(FloatBuffer buf) {

            goFigure();
            buf.rewind();
            float delay = delayTime * SAMPLE_RATE;
            float ldelay = lastdelay;

            int rnlen = workArea.length;
            int pos = rovepos;
            float delta = (delay - ldelay) / N_FRAMES;

            float r, s, a, b, o;
            int ri;
                for (int i = 0; i < N_FRAMES; i++) {
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

}

