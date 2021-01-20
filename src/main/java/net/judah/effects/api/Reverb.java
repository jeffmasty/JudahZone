package net.judah.effects.api;

import java.nio.FloatBuffer;

public interface Reverb extends Effect {

    public enum Settings {
        Room, Damp, Wet
    }

    void initialize(int sampleRate, int bufferSize);

    @Override
    void setActive(boolean active);
    @Override
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

    void setWet(float wet);
    float getWet();

    /** if true, process() must be implemented */
    boolean isInternal();
    public void process(FloatBuffer buf);

}
