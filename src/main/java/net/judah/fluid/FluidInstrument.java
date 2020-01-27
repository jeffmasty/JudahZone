package net.judah.fluid;

public class FluidInstrument {
	public final int group;
	public final int index;
	public final String name;

	public FluidInstrument (int group, int index, String name) {
		this.group = group;
		this.index = index;
		this.name = name;
	}

	/** @param fluidString <pre>
	  	000-124 Telephone
		000-125 Helicopter
		000-126 Applause
		000-127 Gun Shot
		008-004 Detuned EP 1 </pre>*/
	public FluidInstrument(String fluidString)  {
			String[] split = fluidString.split(" ");
			String[] numbers = split[0].split("-");
			group = Integer.parseInt(numbers[0]);
			index = Integer.parseInt(numbers[1]);
			name = fluidString.replace(split[0], "").trim();
		}

	@Override
	public String toString() {
		return group + "-" + index + " " + name;
	}

}
