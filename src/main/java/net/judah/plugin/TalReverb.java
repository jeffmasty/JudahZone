package net.judah.plugin;

import java.nio.FloatBuffer;

import lombok.Getter;
import net.judah.mixer.bus.Reverb;
import net.judah.util.Console;


/**
 *  Carla params: 3 (roomsize), 4 (width), and 6 (highcut)*/
/** If active output audio ports pass through Reverb before hitting the speakers.
 *  Talks to the TAL3 lv2 reverb plugin over Carla's OSC interface */
public class TalReverb implements Reverb {
    public static final int PARAM_ROOMSIZE = 3;
    public static final int PARAM_WIDTH = 4;
    public static final int PARAM_LOWPASS = 6;

    private final Carla carla;
    private final int pluginIdx;

    @Getter private boolean active;
    @Getter private float roomSize = 0.33f;
    @Getter private float damp = 0.25f;
    @Getter private float width = 0.588f;

    TalReverb(Carla carla, Plugin plugin) {
        this.carla = carla;
        this.pluginIdx = plugin.getIndex();
    }

    @Override
    public void initialize(int sampleRate, int bufferSize) {

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
            carla.setParameterValue(pluginIdx, 4, size);
            this.roomSize = size;
        } catch (Exception e) { Console.warn(e); }
    }

    @Override
    public void setDamp(float damp) {

        try {
            carla.setParameterValue(pluginIdx, 7, damp * -1 + 1);
            this.damp = damp;
        } catch (Exception e) { Console.warn(e); }
    }

    @Override
    public void setWidth(float width) {
        try {
            carla.setParameterValue(pluginIdx, 8, width);
            this.width = width;
        } catch (Exception e) { Console.warn(e); }

    }

}
