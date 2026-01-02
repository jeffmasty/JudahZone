package net.judah.looper;

import judahzone.api.FX;

/**TODO
 * Adapter that exposes a Loop as an Effect (RTEffect).
 * Delegates process(...) to the Loop instance.
 */
public final class LoopEffectAdapter implements FX.RTFX {

	// public enum Settings{ CaptureState /*boolean*/ , LoopType }

    private final Loop loop;

    public LoopEffectAdapter(Loop loop) {
        this.loop = loop;
    }

    @Override
    public String getName() {
        return loop.getName();
    }

    @Override
    public void reset() {
    	// loop.rewind()
    }

    @Override
    public void activate() {
        // no-op, Loop lifecycle is managed elsewhere?
    }

    @Override
    public int getParamCount() {
    	// TODO
        return 0;
    }

    @Override
    public void set(int idx, int value) {
        // no-op; add mapping if you want param control (e.g. record/mute)
    }

    @Override
    public int get(int idx) {
        return 0;
    }

    @Override
    public void process(float[] left, float[] right) {
        // Delegate to Loop's process method (Loop already matches signature)
        loop.process(left, right);
    }
}