package net.judah.api;

import java.nio.FloatBuffer;

import net.judah.util.Constants;

public interface Effect {

	int SAMPLE_RATE = Constants.sampleRate();
	int N_FRAMES = Constants.bufSize();

    String getName();

    default void reset() {}

    default void activate() {}

    int getParamCount();

    /**@param idx parameter setting to change
     * @param new value scaled from 0 to 100 */
    void set(int idx, int value);

    /**@return value of setting idx scaled from 0 to 100 */
    int get(int idx);

    /** do the work
     * @param right null for mono effect */
    void process(FloatBuffer left, FloatBuffer right);

    public interface RTEffect extends Effect { }

}

