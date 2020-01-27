package net.judah.fluid;

import java.util.ArrayList;


// TODO thread safe
public class Instruments extends ArrayList<FluidInstrument> {
	private static final long serialVersionUID = -22345012468182682L;

	public int getMaxPreset(int bank) {
		int max = -1;
		for (FluidInstrument i : this) {
			if (i.group == bank)
				if (i.index > max)
					max = i.index;
		}
		return max;
	}

	public String getDescription(int index) {
		return get(index).name;
	}



}
