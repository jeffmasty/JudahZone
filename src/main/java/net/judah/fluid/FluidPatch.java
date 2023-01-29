package net.judah.fluid;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class FluidPatch {
	public final int group;
	public final int index;
	public final String name;

	@Override
	public String toString() {
		return name;
	}

}
