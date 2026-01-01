package net.judah.fx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import judahzone.api.Effect.RTEffect;

public abstract class Reverb implements RTEffect {

    public enum Settings {
        Room, Damp, Wet, Width
    }
    private static List<String> names = settingNames();
    private static List<String> settingNames() {
    	ArrayList<String> build = new ArrayList<String>();
    	for (Settings s : Settings.values())
    		build.add(s.name());
    	return Collections.unmodifiableList(build);
    }

    @Override public final String getName() { return Reverb.class.getSimpleName(); }
    @Override public final List<String> getSettingNames() { return names; }
    @Override public final int getParamCount() { return Settings.values().length; }

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



}
