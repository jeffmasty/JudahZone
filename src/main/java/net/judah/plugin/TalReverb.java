package net.judah.plugin;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import net.judah.effects.api.Reverb;
import net.judah.util.Console;
import net.judah.util.Constants;


/**
 *  Carla params: 3 (roomsize), 4 (width), and 6 (highcut)*/
/** If active output audio ports pass through Reverb before hitting the speakers.
 *  Talks to the TAL3 lv2 reverb plugin over Carla's OSC interface */
public class TalReverb implements Reverb {
    public static final int PARAM_WET = 3;
    public static final int PARAM_ROOMSIZE = 4;
    public static final int PARAM_LOWPASS = 7;
    public static final int PARAM_WIDTH = 8;

    private static final float defRoom = 0.25f;
    private static final float defDamp = 0.3f;
    private static final float defWidth = 0.588f;
    private static final float defWet = 0.3f;
    public static final String NAME = "talReverb";

    private final Carla carla;
    private final int pluginIdx;

    @Getter private boolean active;

    // static because all instances share the same external plugin
    private static float roomSize = defRoom;
    private static float damp = defDamp;
    private static float width = defWidth;
    private static float wet = defWet;

    public TalReverb(Carla carla, Plugin plugin) {
        this.carla = carla;
        this.pluginIdx = plugin.getIndex();
    }

    @Override
    public void initialize(int sampleRate, int bufferSize) {
        setRoomSize(defRoom);
        Constants.timer(30, () -> {setWet(defWet);});
        Constants.timer(60, () -> {setDamp(defDamp);});
        Constants.timer(90, () -> {setWidth(defWidth);});
    }

    @Override public boolean isInternal() { return false; }
    @Override public void process(FloatBuffer buf) { /* no-op (external) */ }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void setRoomSize(float size) {
        try {
            carla.setParameterValue(pluginIdx, PARAM_ROOMSIZE, size);
            TalReverb.roomSize = size;
        } catch (Exception e) { Console.warn(e); }
    }

    @Override
    public void setDamp(float damp) {

        try {
            carla.setParameterValue(pluginIdx, PARAM_LOWPASS, damp * -1 + 1);
            TalReverb.damp = damp;
        } catch (Exception e) { Console.warn(e); }
    }

    @Override
    public void setWidth(float width) {
        try {
            carla.setParameterValue(pluginIdx, PARAM_WIDTH, width);
            TalReverb.width = width;
        } catch (Exception e) { Console.warn(e); }

    }

    @Override
    public void setWet(float wet) {
        try {
            carla.setParameterValue(pluginIdx, PARAM_WET, wet);
            TalReverb.wet = wet;
        } catch (Exception e) { Console.warn(e); }
    }

    @Override
    public float getWet() {
        return wet;
    }

    @Override
    public float getRoomSize() {
        return roomSize;
    }

    @Override
    public float getDamp() {
        return damp;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public int getParamCount() {
        return Settings.values().length;
    }

    @Override
    public void set(int idx, float value) {
        if (idx == Settings.Room.ordinal())
            setRoomSize(value);
        else if (idx == Settings.Damp.ordinal())
            setDamp(value);
        else if (idx == Settings.Wet.ordinal())
            setWet(value);
        else throw new InvalidParameterException();
    }
    @Override
    public Number get(int idx) {
        if (idx == Settings.Room.ordinal())
            return getRoomSize();
        if (idx == Settings.Damp.ordinal())
            return getDamp();
        if (idx == Settings.Wet.ordinal())
            return getWet();
        throw new InvalidParameterException();
    }

}
