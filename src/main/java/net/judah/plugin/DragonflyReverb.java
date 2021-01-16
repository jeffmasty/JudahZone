package net.judah.plugin;

import java.nio.FloatBuffer;

import lombok.Getter;
import net.judah.mixer.bus.Reverb;
import net.judah.util.Console;

/* Ports:
    Dragonfly Plate Reverb:Audio Input 1
    Dragonfly Plate Reverb:Audio Input 2
    Dragonfly Plate Reverb:Audio Output 1
    Dragonfly Plate Reverb:Audio Output 2  */

/** passes output audio ports through Reverb before hitting the speakers.
 *  Carla params: 3 (roomsize), 4 (width), and 6 (highcut)*/
public class DragonflyReverb implements Reverb {
    public static final int PARAM_ROOMSIZE = 3;
    public static final int PARAM_WIDTH = 4;
    public static final int PARAM_LOWPASS = 6;

    private final Carla carla;
    private final int pluginIdx;

    public static final String IN_LEFT = "Dragonfly Plate Reverb:Audio Input 1";
    public static final String IN_RIGHT = "Dragonfly Plate Reverb:Audio Input 2";
    public static final String OUT_LEFT = "Dragonfly Plate Reverb:Audio Output 1";
    public static final String OUT_RIGHT = "Dragonfly Plate Reverb:Audio Output 2";

    @Getter private boolean active;
    @Getter private float roomSize;
    @Getter private float damp;
    @Getter private float width;

    DragonflyReverb(Carla carla, Plugin plugin) {
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
        // Minimum: 10.0 Maximum: 60.0
        int value = Math.round(size * 50 + 10);
        if (value < 10) value = 10;
        if (value > 60) value = 60;
        try {
            carla.setParameterValue(pluginIdx, 3, value);
            this.roomSize = size;
        } catch (Exception e) { Console.warn(e); }
    }

    @Override
    public void setDamp(float damp) {
        // default 10k  min: 1000 Max 11000
        int value = Math.round(damp * -10000) + 11000;

        if (value < 1000) {
            Console.info("oops on " + damp + " ..." + value);
            value = 1000;
        }
        if (value > 11000) {
            Console.info("oops on " + damp + " ..." + value);
            value = 11000;
        }
        try {
            carla.setParameterValue(pluginIdx, 6, value);
            this.damp = damp;
        } catch (Exception e) { Console.warn(e); }
    }

    @Override
    public void setWidth(float width) {
        // Minimum: 50.0 Maximum: 150.0
        int value = Math.round(width * 100 + 50);
        if (value < 50) value = 50;
        if (value > 150) value = 150;
        try {
            carla.setParameterValue(pluginIdx, 4, value);
            this.width = width;
        } catch (Exception e) { Console.warn(e); }

    }

}
