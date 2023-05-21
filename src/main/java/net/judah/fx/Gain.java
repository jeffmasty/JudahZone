package net.judah.fx;

import java.security.InvalidParameterException;

import lombok.Getter;

@Getter
public class Gain implements Effect {
	public static final int VOLUME = 0;
	public static final int PAN = 1;

	private final String name = "Gain";
	private float gain = 0.5f;
	private float stereo = 0.5f;

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
		if (!active)
			stereo = 0.5f;
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
	    return gain * (1 - stereo);
	}
	public float getRight() {
	    return gain * stereo;
	}


}
