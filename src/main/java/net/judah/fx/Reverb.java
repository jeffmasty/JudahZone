package net.judah.fx;

public abstract class Reverb implements Effect {

    public enum Settings {
        Room, Damp, Wet, Width // Width TODO ++ !!
    }

    @Override public String getName() {
        return Reverb.class.getSimpleName();
    }

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

    @Override public int getParamCount() {
        return Settings.values().length;
    }


}
