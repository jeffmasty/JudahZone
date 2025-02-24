package net.judah.synth.taco;

import lombok.RequiredArgsConstructor;
import net.judah.fx.Filter;

/** crank up Resonance on CutFilters */
@RequiredArgsConstructor
public class ModWheel {

	private final Filter lo;
	private final Filter hi;
	private Float oldHi;
	private Float oldLo;
	
	void mod(int data2) {
		if (data2 == 0) {
			if (oldLo != null)
				lo.setResonance(oldLo);
			if (oldHi != null)
				hi.setResonance(oldHi);
			oldHi = oldLo = null;
			return;
		}
		if (oldHi == null) {
			oldHi = hi.getResonance();
			oldLo = lo.getResonance();
		}
		float newLo = oldLo + data2 * (33 - oldLo) / 127f;
		float newHi = oldHi + data2 * (33 - oldHi) / 127f;
		lo.setResonance(newLo);
		hi.setResonance(newHi);
	}
	
}
