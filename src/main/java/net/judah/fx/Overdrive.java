package net.judah.fx;
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 */
import java.nio.FloatBuffer;

import lombok.Getter;
import lombok.Setter;
import net.judah.util.Constants;

/** A single channel unit that processes audio through a simple overdrive effect. */
public final class Overdrive implements Effect {
    private static final double LOG2 = 1.0 / Math.log(2);

    @Getter @Setter boolean active;
    @Getter private float drive = 0.06f;
    private final float makupGain = 1.5f;
    private int nframes;
    
    public Overdrive() {
        initialize(Constants.sampleRate(), Constants.bufSize());
    }

    public void initialize(int samplerate, int buffersize) {
        this.nframes = buffersize;
    }

    @Override public String getName() {
        return Overdrive.class.getSimpleName();
    }

    @Override public int getParamCount() {
        return 1;
    }

    @Override public int get(int idx) {
    	for (int i = 0; i < Constants.getReverseLog().length; i++)
    		if (drive < Constants.getReverseLog()[i])
    			return i;
    	return 0;
    }

    @Override public void set(int idx, int value) {
    	value--;
    	if (value <= 0)
    		setDrive(0);
    	if (value >= 99)
    		setDrive(1);
    	else 
    		setDrive(Constants.logarithmic(value, 0, 1));
    }

    public void setDrive(float drive) {
        this.drive = drive < 0 ? 0 : drive > 1 ? 1 : drive;
    }

    public void processAdd(FloatBuffer buf) {
        buf.rewind();
        double preMul = drive * 99 + 1;
        double postMul = 1 / (Math.log(preMul * 2) * LOG2);
        for (int i = 0; i < nframes; i++) 
			buf.put( makupGain * (float) (Math.atan(buf.get(i) * preMul) * postMul));
    }

    
}

