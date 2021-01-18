package net.judah.mixer.bus;

import java.nio.FloatBuffer;

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Linking this work statically or dynamically with other modules is making a
 * combined work based on this work. Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this work give you permission
 * to link this work with independent modules to produce an executable,
 * regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that
 * you also meet, for each linked independent module, the terms and conditions of
 * the license of that module. An independent module is a module which is not
 * derived from or based on this work. If you modify this work, you may extend
 * this exception to your version of the work, but you are not obligated to do so.
 * If you do not wish to do so, delete this exception statement from your version.
 *
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 *  *
 *
 * Derived from code in Gervill / OpenJDK
 *
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;
import net.judah.util.Constants;

/**
 * Delay op. Delay time and feedback are variable. Changes in delay time are
 * interpolated over the period of one buffer.
 *
 * This is a mono op and requires one input and output channel. Input and output
 * buffers may be the same.
 *
 * @author Neil C Smith (derived from code by Karl Helgason)
 */
public class Delay {

    private final static float DEF_SRATE = 48000;
    private final static float DEF_MAX_DELAY = 1.3f;
    public final static float DEF_TIME = 0.12f;

    private float samplerate;
    private float nframes; // jack buffer size
    private float delaytime = DEF_TIME;
    private float maxdelay;
    private float feedback = 0.36f;

    @Getter @Setter private boolean active;
    private final VariableDelayOp left;
    private final VariableDelayOp right;

    public Delay() {
        this(Constants._SAMPLERATE, Constants._BUFSIZE, DEF_MAX_DELAY);
        reset();
    }

    public Delay(float samplerate, int bufferSize, float maxdelay) {
        if (maxdelay > 0) {
            this.maxdelay = maxdelay;
        }
        this.samplerate = DEF_SRATE;
        if (samplerate < 1) {
            throw new IllegalArgumentException();
        }
        this.samplerate = samplerate;
        this.nframes = bufferSize;

        int delayBufSize = (int) (maxdelay * samplerate) + 10;
        left = new VariableDelayOp(delayBufSize);
        right = new VariableDelayOp(delayBufSize);
    }

    public void setDelay(float time) {
        if (time < 0 || time > maxdelay) {
            throw new IllegalArgumentException();
        }
        this.delaytime = time;
    }

    public float getDelay() {
        return this.delaytime;
    }

    public float getMaxDelay() {
        return this.maxdelay;
    }

    public void setFeedback(float feedback) {
        if (feedback < 0 || feedback > 1) {
            throw new IllegalArgumentException();
        }
        this.feedback = feedback;
    }

    public float getFeedback() {
        return this.feedback;
    }

	public void reset() {
        active = false;
	    if (left.delaybuffer != null)
            Arrays.fill(left.delaybuffer, 0);
        if (right.delaybuffer != null)
            Arrays.fill(right.delaybuffer, 0);
        delaytime = .12f;
        feedback = 0.36f;
    }

	public void processReplace(FloatBuffer in, FloatBuffer out, boolean isLeft) {
        if (isLeft) left.process(in, out, true);
        else right.process(in, out, true);
    }

	public void processAdd(FloatBuffer in, FloatBuffer out, boolean isLeft) {
		if (isLeft) left.process(in, out, false);
		else right.process(in, out, false);
    }


	private class VariableDelayOp {
	    private float[] delaybuffer;
	    private int rovepos = 0;
	    private float lastdelay;

	    public VariableDelayOp(int bufSize) {
	        this.delaybuffer = new float[bufSize];
	        this.rovepos = 0;
	        this.lastdelay = 0;
		}

    public void process(FloatBuffer in, FloatBuffer out, boolean replace) {

    	out.rewind();
        float[] buf = this.delaybuffer;
        if (buf == null) {
            // not been initialized
            throw new IllegalStateException();
        }
        float srate = samplerate;
        float delay = delaytime * srate;
        float ldelay = this.lastdelay;
        float fb = feedback;
        int rnlen = buf.length;
        int pos = this.rovepos;
        float delta = (delay - ldelay) / nframes;

        float r, s, a, b, o;
        int ri;
        if (replace) {
            for (int i = 0; i < nframes; i++) {
                r = pos - (ldelay + 2) + rnlen;
                ri = (int) r;
                s = r - ri;
                a = buf[ri % rnlen];
                b = buf[(ri + 1) % rnlen];
                o = a * (1 - s) + b * s;
                buf[pos] = in.get(i) + o * fb;
                out.put(o);
                pos = (pos + 1) % rnlen;
                ldelay += delta;
            }

        } else {
            for (int i = 0; i < nframes; i++) {
                r = pos - (ldelay + 2) + rnlen;
                ri = (int) r;
                s = r - ri;
                a = buf[ri % rnlen];
                b = buf[(ri + 1) % rnlen];
                o = a * (1 - s) + b * s;
                buf[pos] = in.get(i) + o * fb;
                out.put(out.get(i) + o);
                pos = (pos + 1) % rnlen;
                ldelay += delta;
            }
        }
        this.rovepos = pos;
        this.lastdelay = delay;
    }
	}
}



