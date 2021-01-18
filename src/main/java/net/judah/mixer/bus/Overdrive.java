package net.judah.mixer.bus;
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
public final class Overdrive {

    @Getter @Setter boolean active;
    @Getter private double drive;
    private int nframes;

    public Overdrive() {
        initialize(Constants._SAMPLERATE, Constants._BUFSIZE);
        reset();
    }

    public void initialize(int samplerate, int buffersize) {
        this.nframes = buffersize;
    }

    public void setDrive(double drive) {
        this.drive = drive < 0 ? 0 : drive > 1 ? 1 : drive;
    }

    public void reset() {
        drive = 0;
    }

    private final float downGain = 0.5f;
    public void processAdd(FloatBuffer buf) {
        buf.rewind();
        double preMul = drive * 99 + 1;
        double postMul = 1 / (Math.log(preMul * 2) / Math.log(2));
        for (int i = 0; i < nframes; i++) {

            buf.put( downGain * (float) (Math.atan(buf.get(i) * preMul) * postMul));
        }
    }


}

