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

    public enum Settings {
        Drive, Clipping
    }

    @Getter @Setter boolean active;
    private float drive = 0.2f;
    private float makeupGain = 0.65f;
    private float clipping = 0;
    private float diode = 2;
    @Getter String name = Overdrive.class.getSimpleName();
    @Getter int paramCount = Settings.values().length;

    @Override public int get(int idx) {
    	if (idx == 0) {
    		if (drive == 0)
    			return 0;
    		if (drive < 0.00002f)
    			return 1;
    		return Constants.reverseLog(drive, 0.2f, 0.7f);
    	} else if (idx == 1)
	    	return (int) (clipping * 100f);

	    return 0;
    }

    @Override public void set(int idx, int value) {
    	if (idx == 0) {
	    	if (value < 2)
	    		drive = 0.00001f;
	    	else
	    		drive = Constants.logarithmic(value, 0.2f, 0.7f);
    	} else {
    		clipping = value * 0.01f;
            // as clipping goes to zero, limit goes to 2
            // as clipping goes to 1, limit goes to 1
    		diode = 1 + (3 - 2 * clipping);
    	}
    }

    @Override
	public void process(FloatBuffer left, FloatBuffer right) {
    	processAdd(left);
    	processAdd(right);
    }

    private void processAdd(FloatBuffer buf) {

        double preMul = drive * 99 + 1;
        double postMul = 1 / (Math.log(preMul * 2) * LOG2);
        float gain = makeupGain;

        buf.rewind();
        if (clipping < 0.01f)
	        while (buf.hasRemaining()) {
	            float processedValue = gain * (float) (Math.atan(buf.get() * preMul) * postMul);
	            buf.put(buf.position() - 1, processedValue);
	        }
        else {
	        while (buf.hasRemaining()) {
	            float value = buf.get();
	            float processedValue = gain * (float) (Math.atan(value * preMul) * postMul);
	            float max = diode * value;
	            buf.put(buf.position() - 1, Math.abs(processedValue) < Math.abs(max) ? processedValue : max);

	        }
        }
    }

}

// clipping https://www.youtube.com/watch?v=rnvEA7SOaSA

/*https://github.com/martinpenberthy/JUCEGuitarAmpBasic?tab=readme-ov-file
 Tanh
std::tanh (x);

 x/abs(x)+1
x / (std::abs(x) + 1)

Amp2
(x * (std::abs(x) + 0.9f)) * 1.5f / (x * x + (0.3f) * (0.1f / std::abs(x)) + 1.0f) * 0.6f;

Atan
std::atan(x);

HalfRect
if(x < 0.0f)
    return 0.0f
else
    return x;

Amp1
((x / (std::abs(x) + 0.9f) * 1.5f ) / (x * x + (0.0f - 1.0f) * std::abs(x) + 1.0f)) * 0.7f;

Waveshaper2:
x / (std::abs(x) + 1)

Waveshaper3:
x / (std::abs(x) + 2)
 */
