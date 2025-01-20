package net.judah.fx;

import java.nio.FloatBuffer;

import net.judah.util.Constants;

public interface Effect {

	int SAMPLE_RATE = Constants.sampleRate();
	int N_FRAMES = Constants.bufSize();

    String getName();

    void setActive(boolean active);

    boolean isActive();

    int getParamCount();

    /**@return value of setting idx scaled from 0 to 100 */
    void set(int idx, int value);

    /**@param idx parameter setting to change
     * @return changed value scaled from 0 to 100*/
    int get(int idx);

    /** do the work
     * @param right null for mono effect */
    void process(FloatBuffer left, FloatBuffer right);

}
