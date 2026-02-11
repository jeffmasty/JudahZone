package net.judah.drums.synth;

import judahzone.data.Env;
import judahzone.data.Filter;
import judahzone.data.Stage;
import judahzone.fx.EQ.EqBand;
import judahzone.fx.Gain;
import judahzone.fx.op.NoiseGen.Colour;
import net.judah.drums.KitDB.BaseParam;
import net.judah.gui.MainFrame;


/** Maps GUI selections (tab/param) to DrumOsc parameter setters. */
public record DrumParams(DrumOsc drum, int tabIdx, int paramIdx, int value) {

	public static record Settings(
			Stage gainStage,
			Env env,
			Freqs freqs,
			Colour colour,
			String[] names) { } // custom param names for each drum type

	public static record Freqs(Filter lowCut, Filter body, Filter hiCut) { } // setup
	public static enum FilterParam { LowCut, HiCut, LowRes, HiRes } // gui
	public static enum DrumParam { Param1, Param2, Param3, Pitch } // gui

	/** Route (tab, paramIdx, value) to the correct DrumOsc setter. */
	public static void set(DrumParams data) {
		switch(data.tabIdx()) {
			case 0: setBase(data.drum(), BaseParam.values()[data.paramIdx()], data.value()); break;
			case 1: setFilter(data.drum(), FilterParam.values()[data.paramIdx()], data.value()); break;
			case 2: setCustom(data.drum(), DrumParam.values()[data.paramIdx()], data.value()); break;
		}
		MainFrame.update(data);
	}

	/** Get current value for display/sync. */
	public static int get(DrumOsc drum, int tabIdx, int paramIdx) {
		switch(tabIdx) {
			case 0: return getBase(drum, BaseParam.values()[paramIdx]);
			case 1: return getFilter(drum, FilterParam.values()[paramIdx]);
			case 2: return getCustom(drum, DrumParam.values()[paramIdx]);
			default: return 0;
		}
	}

	private static void setBase(DrumOsc drum, BaseParam p, int v) {
		switch(p) {
			case Vol: drum.getGain().set(Gain.VOLUME, v); break;
			case Pan: drum.getGain().set(Gain.PAN, v); break;
			case Attack: drum.setAttack(v); break;
			case Decay: drum.setDecay(v); break;
		}
	}

	private static int getBase(DrumOsc drum, BaseParam p) {
		switch(p) {
			case Vol: return drum.getGain().get(Gain.VOLUME);
			case Pan: return drum.getGain().get(Gain.PAN);
			case Attack: return drum.getAttack();
			case Decay: return drum.getDecay();
			default: return 0;
		}
	}

	private static void setFilter(DrumOsc drum, FilterParam p, int v) {
		switch(p) {
			case LowCut: drum.setHzKnob(EqBand.Bass, v); break;
			case HiCut: drum.setHzKnob(EqBand.High, v); break;
			case LowRes: drum.setResonanceKnob(EqBand.Bass, v); break;
			case HiRes: drum.setResonanceKnob(EqBand.High, v); break;
		}
	}

	private static int getFilter(DrumOsc drum, FilterParam p) {
		// Return knob-scale values so GUI knobs receive the same range used by setters.
		switch(p) {
			case LowCut: return drum.getHzKnob(EqBand.Bass);
			case HiCut: return drum.getHzKnob(EqBand.High);
			case LowRes: return drum.getResonanceKnob(EqBand.Bass);
			case HiRes: return drum.getResonanceKnob(EqBand.High);
			default: return 0;
		}
	}

	private static void setCustom(DrumOsc drum, DrumParam p, int v) {
		switch(p) {
			case Pitch:
				drum.setHzKnob(EqBand.Mid, v); break;
			case Param1, Param2, Param3:
				drum.set(p, v);
				break;
		}
	}

	private static int getCustom(DrumOsc drum, DrumParam p) {
	    switch(p) {
	        case Pitch:
	        	return drum.getPitchKnob();
	        case Param1, Param2, Param3:
	            return drum.get(p);
	        default: return 0;
	    }
	}

}
