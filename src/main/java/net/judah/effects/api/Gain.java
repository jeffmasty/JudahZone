package net.judah.effects.api;

import java.security.InvalidParameterException;

import lombok.Getter;
import lombok.Setter;

public class Gain implements Effect {
	public static final int VOLUME = 0;
	public static final int PAN = 1;
	@Getter final String name = "Gain";

	@Getter private int vol = 50;
	@Getter @Setter private int pan = 50;
	@Getter private float gain = vol * 0.01f;
	
	public void setVol(int volume) {
		if (volume < 0) volume = 0;
		if (volume > 100) volume = 100;
		this.vol = volume;
		this.gain = vol * 0.01f;
	}
	
	public void setGain(float gain) {
		setVol((int) (gain * 100));
	}
	
	@Override
	public boolean isActive() {
		return pan != 0.5f;
	}

	@Override
	public void setActive(boolean active) {
		// noOp
	}

	@Override
	public void set(int idx, int value) {
		if (value < 0 || value > 100)
			throw new InvalidParameterException(
					this.getClass().getSimpleName() + " " + idx + " " + value);
		if (idx == VOLUME)
			vol = value;
		else if (idx == PAN)
			pan = value;
		else throw new InvalidParameterException("idx " + idx);
	}

	@Override
	public int get(int idx) {
		if (idx == VOLUME)
			return vol;
		if (idx == PAN)
			return pan;
		throw new InvalidParameterException("idx " + idx);
	}

	@Override
	public int getParamCount() {
		return 2;
	}

}
