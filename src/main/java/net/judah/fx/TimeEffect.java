package net.judah.fx;

import net.judah.JudahZone;
import net.judah.util.Constants;

public interface TimeEffect extends Effect {

	static String[] TYPE = {"1/8", "1/4", "3/8", "1/2"};

	static int indexOf(String type) {
		for (int i = 0; i < TYPE.length; i++)
			if (TYPE[i].equals(type))
				return i;
		return 0;
	}
	
	static float unit() {
		return 2 * Constants.millisPerBeat(
				JudahZone.getClock().getTempo()) / (float)JudahZone.getClock().getSubdivision();
	}
	
	void setType(String type);
	String getType();
	
	void setSync(boolean sync);
	boolean isSync();
	void sync(float unit);
	void sync();
	
}
