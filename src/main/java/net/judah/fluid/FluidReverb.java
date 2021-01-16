package net.judah.fluid;

import java.nio.FloatBuffer;

import lombok.Getter;
import net.judah.mixer.bus.Reverb;

public class FluidReverb implements Reverb {

    private final FluidSynth fluid;

    @Getter private boolean active;
    @Getter private float roomSize;
    @Getter private float damp;
    @Getter private float width;

    public FluidReverb(FluidSynth synth) {
        this.fluid = synth;
        initialize(1, 1);
    }

    /** Fluid Synth already started, we will initialize size/damp settings and activate */
    @Override
    public void initialize(int sampleRate, int bufferSize) {
        setRoomSize(0.4f);
        setDamp(0.4f);
        setWidth(0.7f);
        setActive(true);
    }

    @Override public boolean isInternal() { return false; }
    @Override public void process(FloatBuffer buf) { /* no-op (external) */ }

    @Override
    public void setActive(boolean active) {
        this.active = active;
        fluid.sendCommand("reverb " + (active ? "on" : "off"));
    }

    @Override
    public void setWidth(float val) {
        if (val > 1) val = 1;
        if (val < 0) val = 0;
        fluid.sendCommand("set synth.reverb.width " + val);
        this.width= val;
    }

    @Override
    public void setRoomSize(float val) {
        if (val > 1) val = 1;
        if (val < 0) val = 0;
        fluid.sendCommand("set synth.reverb.room-size " + val);
        this.roomSize = val;
    }

    @Override
    public void setDamp(float val) {
        if (val > 1) val = 1;
        if (val < 0) val = 0;
        fluid.sendCommand("set synth.reverb.damp " + val);
        this.damp = val;
    }

}
