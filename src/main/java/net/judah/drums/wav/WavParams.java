package net.judah.drums.wav;

import judahzone.fx.Gain;
import net.judah.drums.KitDB.BaseParam;
import net.judah.gui.MainFrame;

@Deprecated
public record WavParams(WavDrum drum, BaseParam base, int v) {

	/** Drum samples only have base params. */
	@Deprecated
	public static void set(WavParams s) {
		switch(s.base) {
			case Vol: s.drum().getGain().set(Gain.VOLUME, s.v()); break;
			case Pan: s.drum().getGain().set(Gain.PAN, s.v()); break;
			case Attack: s.drum().getEnv().setAttack(s.v()); break;
			case Decay: s.drum().getEnv().setDecay(s.v()); break;
		}
		MainFrame.update(s);
	}

	/** Get current value for DrumSample display/sync. */
	@Deprecated
	public static int get(WavDrum drum, BaseParam p) {
		switch(p) {
			case Vol: return drum.getVol();
			case Pan: return drum.getPan();
			case Attack: return drum.getEnv().getAttack();
			case Decay: return drum.getEnv().getDecay();
			default: return 0;
		}
	}


}
