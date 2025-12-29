package net.judah.synth.taco;

import judahzone.util.Constants;
import lombok.RequiredArgsConstructor;
import net.judah.fx.MonoFilter;
import net.judah.gui.MainFrame;

/** crank up Resonance on Filters */
@RequiredArgsConstructor
public class ModWheel {

	private static final int RESONANCE = MonoFilter.Settings.Resonance.ordinal();

	private final TacoSynth synth;
	private final MonoFilter lo;
	private final MonoFilter hi;

	private Integer oldHi;
	private Integer oldLo;

	void dragged(int data2) {

		if (data2 == 0) {
			if (oldLo != null)
				lo.set(RESONANCE, oldLo);
			if (oldHi != null)
				hi.set(RESONANCE, oldHi);
			oldHi = null;
			oldLo = null;
			MainFrame.update(synth.getKnobs());
			return;
		}

		if (oldHi == null) {
			oldHi = hi.get(RESONANCE);
			oldLo = lo.get(RESONANCE);
		}

		float ratio = data2 * Constants.TO_1;
		int newLo = oldLo + (int) (100 * ratio);
		newLo = oldLo + Math.round((100 - oldLo) * ratio);
		if (newLo > 100)
			newLo = 100;
		lo.set(RESONANCE, newLo);
		int newHi = oldHi + (int) (100 * ratio);
		if (newHi > 100)
			newHi = 100;
		hi.set(RESONANCE, newHi);
		MainFrame.update(synth.getKnobs());

	}

}
