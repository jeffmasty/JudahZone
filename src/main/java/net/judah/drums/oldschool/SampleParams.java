package net.judah.drums.oldschool;

import judahzone.fx.Gain;
import net.judah.drums.KitDB.BaseParam;
import net.judah.gui.MainFrame;

public record SampleParams(DrumSample drum, BaseParam base, int v) {

	/** Drum samples only have base params. */
	public static void set(SampleParams s) {
		switch(s.base) {
			case Vol: s.drum().getGain().set(Gain.VOLUME, s.v()); break;
			case Pan: s.drum().getGain().set(Gain.PAN, s.v()); break;
			case Attack: s.drum().setAttack(s.v()); break;
			case Decay: s.drum().setDecay(s.v()); break;
		}
		MainFrame.update(s);
	}

	/** Get current value for DrumSample display/sync. */
	public static int get(DrumSample drum, BaseParam p) {
		switch(p) {
			case Vol: return drum.getVol();
			case Pan: return drum.getPan();
			case Attack: return drum.getAttack();
			case Decay: return drum.getDecay();
			default: return 0;
		}
	}


}
