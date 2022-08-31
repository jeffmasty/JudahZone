package net.judah.plugin;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import net.judah.effects.api.Reverb;
import net.judah.util.Constants;
import net.judah.util.RTLogger;


/**
 *  Carla params: 3 (roomsize), 4 (width), and 6 (highcut)*/
/** If active output audio ports pass through Reverb before hitting the speakers.
 *  Talks to the TAL3 lv2 reverb plugin over Carla's OSC interface */
public class TalReverb extends Reverb {
    public static final int PARAM_WET = 3;
    public static final int PARAM_ROOMSIZE = 4;
    public static final int PARAM_LOWPASS = 7;
    public static final int PARAM_WIDTH = 8;

    private static final float defRoom = 0.25f;
    private static final float defDamp = 0.75f;
    private static final float defWidth = 0.588f;
    private static final float defWet = 0.3f;
    public static final String NAME = "talReverb";

    private final Carla carla;
    private final int pluginIdx;

    @Getter private boolean active;
    private float roomSize = defRoom;
    private float damp = defDamp;
    private float width = defWidth;
    private float wet = defWet;

    public TalReverb(Carla carla, Plugin plugin, long startupWait) {
        this.carla = carla;
        this.pluginIdx = plugin.getIndex();
        Constants.timer(startupWait, () ->{ // wait for host to start, set defaults
        	initialize(0,0);});
    }

    @Override
    public void initialize(int sampleRate, int bufferSize) {
//        setRoomSize(defRoom);
//        Constants.timer(55, () -> {setWet(defWet);});
//        Constants.timer(110, () -> {setDamp(defDamp);});
//        Constants.timer(165, () -> {setWidth(defWidth);});
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
            roomSize = size;
        } catch (Exception e) { RTLogger.warn(this, e); }
    }

    @Override
    public void setDamp(float damp) {

        try {
            carla.setParameterValue(pluginIdx, PARAM_LOWPASS, damp * -1 + 1);
            this.damp = damp;
        } catch (Exception e) { RTLogger.warn(this, e); }
    }

    @Override
    public void setWidth(float width) {
        try {
            carla.setParameterValue(pluginIdx, PARAM_WIDTH, width);
            this.width = width;
        } catch (Exception e) { RTLogger.warn(this, e); }

    }

    @Override
    public void setWet(float wet) {
        try {
            carla.setParameterValue(pluginIdx, PARAM_WET, wet);
            this.wet = wet;
        } catch (Exception e) { RTLogger.warn(this, e); }
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
    public int get(int idx) {
        if (idx == Settings.Room.ordinal())
            return Math.round(getRoomSize() * 100);
        if (idx == Settings.Damp.ordinal())
            return Math.round(getDamp() * 100);
        if (idx == Settings.Wet.ordinal())
            return Math.round(getWet() * 100);
        throw new InvalidParameterException();
    }

    @Override
    public void set(int idx, int value) {
        if (idx == Settings.Room.ordinal())
            setRoomSize(value / 100f);
        else if (idx == Settings.Damp.ordinal())
            setDamp(value / 100f);
        else if (idx == Settings.Wet.ordinal())
            setWet(value / 100f);
        else throw new InvalidParameterException();
    }

}
