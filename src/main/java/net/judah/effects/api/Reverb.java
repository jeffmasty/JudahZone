package net.judah.effects.api;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

public abstract class Reverb implements Effect {

    public enum Settings {
        Room, Damp, Wet
    }

    @Override public String getName() {
        return Reverb.class.getSimpleName();
    }

    public abstract void initialize(int sampleRate, int bufferSize);

    /**@param size 0 to 1 */
    public abstract void setRoomSize(float size);
    public abstract float getRoomSize();

    /**@param dampness 0 to 1 */
    public abstract void setDamp(float dampness);
    public abstract float getDamp();

    /**@param width 0 to 1 */
    public abstract void setWidth(float width);
    public abstract float getWidth();

    public abstract void setWet(float wet);
    public abstract float getWet();

    /** if true, process() must be implemented */
    public abstract boolean isInternal();
    public void process(FloatBuffer buf) {}

    // NO-OPs, overwrite in subclass
    @Override
    public void set(int ordinal, float value) {
        throw new InvalidParameterException();
    }
    @Override
    public float get(int idx) {
        throw new InvalidParameterException();
    }

    @Override
    public int getParamCount() {
        return 0;
    }


}
