package net.judah.fx;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import lombok.Setter;
import net.judah.util.Constants;

@Getter
public class Gain implements Effect {
	public static final int VOLUME = 0;
	public static final int PAN = 1;

	private final String name = "Gain";
	private float gain = 0.5f;
	private float stereo = 0.5f;
	@Setter private float preamp = 1f;

	@Override
	public void set(int idx, int value) {
		if (value < 0 || value > 100)
			throw new InvalidParameterException(
					this.getClass().getSimpleName() + " " + idx + " " + value);
		if (idx == VOLUME)
			setGain(value * 0.01f);
		else if (idx == PAN)
			setPan(value * 0.01f);
		else throw new InvalidParameterException("idx " + idx);
	}

	public void setGain(float g) {
		if (g < 0)
			g = 0;
		if (g > 1)
			g = 1;
		gain = g;
	}
	public void setPan(float p) {
		if (p < 0)
			p = 0;
		if (p > 1)
			p = 1;
		stereo = p;
	}

	@Override
	public boolean isActive() {
		return stereo < 0.49f || stereo > 0.51f;
	}

	@Override
	public void setActive(boolean active) {
		if (!active) stereo = 0.5f;
	}

	@Override
	public int get(int idx) {
		if (idx == VOLUME)
			return (int) (gain * 100);
		if (idx == PAN)
			return (int) (stereo * 100);
		throw new InvalidParameterException("idx " + idx);
	}

	@Override
	public int getParamCount() {
		return 2;
	}

	public float getLeft() {
	    return preamp * gain * (1 - stereo);
	}
	public float getRight() {
	    return preamp * gain * stereo;
	}

	@Override
	public void process(FloatBuffer left, FloatBuffer right) {
		left.rewind();
		if (right == null) { // error state?
			processMono(left);
			return;
		}
		right.rewind();

		float vol = preamp * gain;
		float l = vol * (1 - getStereo());
		float r = vol * getStereo();
		for (int z = 0; z < N_FRAMES; z++) {
			left.put(left.get(z) * l);
			right.put(right.get(z) * r);
		}
	}

	private void processMono(FloatBuffer mono) {
		for (int z = 0; z < Constants.bufSize(); z++)
			mono.put(mono.get(z) * preamp * gain);
	}

	/** preamp and panning */
	public void preamp(FloatBuffer left, FloatBuffer right) {
		final float l = preamp * (1 - getStereo());
		final float r = preamp * getStereo();
		left.rewind(); right.rewind();
	    while (left.hasRemaining())
	        left.put(left.position(), left.get() * l);
	    while (right.hasRemaining())
	        right.put(right.position(), right.get() * r);

	}

	/** gain only */
	public void post(FloatBuffer left, FloatBuffer right) {
		final float fader = this.gain;
		left.rewind(); right.rewind();
	    while (left.hasRemaining())
	    	left.put(left.position(), left.get() * fader);
	    while (right.hasRemaining())
	        right.put(right.position(), right.get() * fader);
	}

	public void reset() {
		gain = 0.5f;
		stereo = 0.5f;
	}
}
