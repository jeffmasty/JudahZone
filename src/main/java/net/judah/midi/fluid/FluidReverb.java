package net.judah.midi.fluid;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import net.judah.fx.Reverb;
import net.judah.util.Constants;

public class FluidReverb extends Reverb {

    private final FluidSynth fluid;

    @Getter private boolean active;
    @Getter private float roomSize;
    @Getter private float damp;
    @Getter private float wet;
    @Getter private float width;

    public FluidReverb(FluidSynth synth) {
        this.fluid = synth;
        initialize(1, 1);
    }

    /** Fluid Synth already started, we will initialize size/damp settings and activate */
    @Override
    public void initialize(int sampleRate, int bufferSize) {
        Constants.timer(2000, () -> setWidth(0.7f));
        Constants.timer(1600, () -> setWet(0.1f));
        Constants.timer(1700, () -> setActive(true));
        Constants.timer(1800, () -> setRoomSize(0.75f));
        Constants.timer(1900, () -> setDamp(0.6f));

    }

    @Override public boolean isInternal() { return false; }
    @Override public void process(FloatBuffer buf) { /* no-op (external) */ }

    @Override
    public void setActive(boolean active) {
    	if (this.active == active) return;
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

    /** */
    @Override
    public void setWet(float val) {
        if (val > 1) val = 1;
        if (val < 0) val = 0;
        fluid.sendCommand("set synth.reverb.level " + val);
        this.wet = val;
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
