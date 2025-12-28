package net.judah.api;

import static net.judah.midi.JudahMidi.getClock;
import static net.judah.util.Constants.millisPerBeat;

public interface TimeEffect extends Effect {

	static float unit() {
		return millisPerBeat(getClock().getTempo()) / (float)getClock().getSubdivision(); // * 2
	}

	static String[] TYPE = {"1/8", "1/4", "3/8", "1/2"};

	static int indexOf(String type) {
		for (int i = 0; i < TYPE.length; i++)
			if (TYPE[i].equals(type))
				return i;
		return 0; // fail
	}

	void setType(String type);
	String getType();

	void setSync(boolean sync);
	boolean isSync();
	void sync(float unit);
	void sync();

}
