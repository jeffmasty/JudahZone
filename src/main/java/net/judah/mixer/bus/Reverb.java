package net.judah.mixer.bus;

import java.nio.FloatBuffer;

public interface Reverb {

    void initialize(int sampleRate, int bufferSize);

    void setActive(boolean active);
    boolean isActive();

    /**@param size 0 to 1 */
    void setRoomSize(float size);
    float getRoomSize();

    /**@param dampness 0 to 1 */
    void setDamp(float dampness);
    float getDamp();

    /**@param width 0 to 1 */
    void setWidth(float width);
    float getWidth();

    /** if true, process() must be implemented */
    boolean isInternal();
    public void process(FloatBuffer buf);


}
