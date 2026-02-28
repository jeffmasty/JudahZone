
package net.judah.drums;

import judahzone.api.Frequency;
import judahzone.filter.Coord;
import judahzone.filter.FilterT;
import judahzone.fx.EQ.EqBand;
import judahzone.fx.Gain;
import judahzone.util.Filters;
import judahzone.util.RTLogger;
import lombok.Getter;
import net.judah.drums.DrumUpdate.Mode;
import net.judah.drums.fm.FMOsc;
import net.judah.drums.noise.NoiseOsc;
import net.judah.drums.pluck.PluckOsc;
import net.judah.drums.pluck.PluckSetup;
import net.judah.gui.MainFrame;
import net.judah.gui.TabZone;
import net.judah.gui.drums.knobs.Kitz;

/** Central routing for UI parameter updates.

  Threading: maybe better to send DrumUpdate to MainFrame (for midi knobs) and then DrumUpdate is received on EDT?
  If there is no midi knobs present, no RT, we are in EDT, then the DrumUpdate can be sent directly to Drama.set()
  	(e.g. from a slider, combo, button, other gui), but then any open DrumFrames also need mechanism of refresh  (Drama?) */
public class Drama {

	public static record Freqs(Coord lowCut, FilterT body, Coord hiCut) { } // setup

	public static enum FilterParam { LowCut, HiCut, LowRes, HiRes, Mid, Gain, Width } // json

	public static enum BASE { Vol, Pan, Attack, Decay;
		private static String[] names;
		public static String[] names() {
			if (names ==  null) {
				names = new String[values().length];
				for (BASE p : values())
					names[p.ordinal()] = p.name();
			}
			return names;
		}
	};
	public static enum FILTERS {LowCut, HiCut, LowRes, HiRes, M_Hz, M_Gain, M_BW;
		private static String[] names;
		public static String[] names() {
			if (names ==  null) {
				names = new String[values().length];
				for (FILTERS p : values())
					names[p.ordinal()] = p.name();
			}
			return names;
		}
	};

	private final Drum drum;

	/** includes "tuning" and "tracking" if drum supports fundamental frequency */
	@Getter protected final String[] extra;

	public Drama(Drum drum, String[] paramNames) {
		this.drum = drum;

		if (paramNames == null || paramNames.length == 0)
			extra = new String[0];
		else {
			int pz = paramNames.length;
			int target = drum == null ? pz : pz + 2; // if pitched, add "Tuning" and "Tracking"
			extra = new String[target];
			for (int i = 0; i < pz; i++)
				extra[i] = paramNames[i];
			if (target > pz) {
				extra[pz] = "Tuning"; // setPitch()
				extra[pz + 1] = "Tracking"; // bool
			}
		}
	}

	/** Route a UI-style param update to the appropriate setter. */
	public void set(DrumUpdate data) {

		int idx = data.paramIdx();
		int v = data.value();

		switch (data.mode()) {
			case BASE -> {
				if (idx == 0)
					drum.getGain().set(Gain.VOLUME, v);
				else if (idx == 1)
					drum.getGain().set(Gain.PAN, v);
				else if (idx == 2)
					drum.getEnv().setAttack(v);
				else if (idx == 3)
					drum.getEnv().setDecay(v);
				else if (idx == 4) // convenience: mid freq param is log scale, so allow direct knob control
					setPitchKnob((byte)v);
				else if (idx == 5) // convenience: mid gain param is +/- 24dB, so allow direct knob control
					setTracking(v != 0);
			}
			case FILTER -> {
				if (idx == 0)
					drum.setHzKnob(EqBand.Bass, v);
				else if (idx == 1)
					drum.setHzKnob(EqBand.High, v);
				else if (idx == 2)
					drum.setResonanceKnob(EqBand.Bass, v);
				else if (idx == 3)
					drum.setResonanceKnob(EqBand.High, v);
				else if (idx == 4)
					drum.setHzKnob(EqBand.Mid, v);
				else if (idx == 5)
					drum.setResonanceKnob(EqBand.Mid, v);
				else if (idx == 6) {
					float bw = Filters.MIN_BANDWIDTH + (v / 100f) * (Filters.MAX_BANDWIDTH - Filters.MIN_BANDWIDTH);
					drum.getMid().setBandwidth(bw);
				}
			}
			case PLUCK -> {
				if (drum instanceof PluckOsc pluck)
					pluck.set(idx, data.value());
				else
					RTLogger.warn(this, drum.getClass().getSimpleName() + "ERROR Pluck param update idx: " + idx + " value: " + v);
			}
			case FM -> {
				if (drum instanceof FMOsc fm)
					fm.set(idx, data.value());
				else
					RTLogger.warn(this, drum.getClass().getSimpleName() + "ERROR FM param update idx: " + idx + " value: " + v);
			}
			case NOISE -> {
				if (drum instanceof NoiseOsc noise)
					noise.set(idx, data.value());
				else
					RTLogger.warn(this, drum.getClass().getSimpleName() + "ERROR Noise param update idx: " + idx + " value: " + v);
			}
		}
		broadcast();

	}

	public void setTracking(boolean selected) {
		if (drum == null) {
			RTLogger.warn(this, "Tracking change on non-pitched drum: " + selected);
			return;
		}
		drum.setSync(selected);
		broadcast();
	}

	private void setPitchKnob(byte value) { // 0 - 127
		if (drum == null) {
			RTLogger.warn(this, "Tuning change on non-pitched drum: " + value);
			return;
		}
		drum.setPitchKnob(value);
	}

	// TODO targeted updates
	public void broadcast(/*DrumUpdate update*/) {
		if (MainFrame.getKnobs() instanceof Kitz kitz)
			kitz.update(drum); // TODO targeted data
		TabZone.update(drum); // TODO targeted data
	}

	public String[] getParamNames(Drum drum, Mode mode) {
		return switch(mode) {
			case BASE -> Drama.BASE.names();
			case FILTER -> Drama.FILTERS.names();
			case FM -> extra; // TODO noise p1,2,3
			case PLUCK -> extra;
			case NOISE -> new String[] {"param1", "param2", "param3"};
			default -> new String[0];
		};
	}

	public String getUnit(Mode mode, int paramIdx) {
		if (mode == Mode.BASE) {
			return switch(paramIdx) {
				case 0 -> "%";
				case 1 -> "L/R";
				case 2, 3 -> "ms";
				case 4 -> "Hz";
				case 5 -> "Sync";
				default -> "";
			};
		}
		if (mode == Mode.FILTER) {
			return switch (FilterParam.values()[paramIdx]) {
				case LowCut, HiCut, Mid -> "Hz";
				case LowRes, HiRes -> "dB";
				case Gain -> "dB";
				case Width -> "Oct.";
			};
		}
		if (mode == Mode.PLUCK) {
			return switch (PluckSetup.Params.values()[paramIdx]) {
				case BURST -> "ms";
				case AMP, HARMONY, COMBS, TAP -> "%";
				case COLOUR -> "Colour"; // TODO enums
				case FEEDBACK, DAMP -> "%";
				default -> "";
			};
		}
		// Noise params (custom) ->  %?
		// FM -> algo
		if (mode == Mode.FM) {
			return "";
		}
		if (mode == Mode.NOISE) {
			return extra[paramIdx];
		}


		return "%";
	}

	/** Read-back helper for UI (returns knob-scale values). */
	public int get(int paramIdx, Mode mode) {
		if (mode == Mode.BASE)
			return getBase(paramIdx);

		if (mode == Mode.FILTER)
			return getFilter(paramIdx);

		if (mode == Mode.PLUCK && drum instanceof PluckOsc pluck)
			return pluck.get(paramIdx);

		if (mode == Mode.FM && drum instanceof FMOsc fm)
			return fm.get(paramIdx);

		if (mode == Mode.NOISE && drum instanceof NoiseOsc noise)
			return noise.get(paramIdx);
		return -1;
	}

	public String val(int paramIdx, Mode mode) {
		if (mode == Mode.BASE)
			return switch (paramIdx) {
				case 0 -> drum.getGain().get(Gain.VOLUME) + "";
				case 1 -> drum.getGain().get(Gain.PAN) + "";
				case 2 -> drum.getEnv().getAttackMs() + "";
				case 3 -> drum.getEnv().getDecayMs() + "";
				case 4 -> drum == null ? "??" : Frequency.toNote(drum.getHz(EqBand.Mid)).toString();
				case 5 -> drum != null && drum.isSync() ? "On" : "Off";
				default -> "";
			};

		if (mode == Mode.FILTER)
			return switch (FilterParam.values()[paramIdx]) {
				case LowCut -> (int)drum.getHz(EqBand.Bass) + "";
				case HiCut -> (int)drum.getHz(EqBand.High) + "";
				case LowRes -> String.format("%.1f", drum.getResonanceKnob(EqBand.Bass) * 0.25f);
				default -> "";
			};

		if (drum != null)
			return drum.val(paramIdx);
		return "???";

	}

	private int getBase(int idx) {
		return switch (idx) {
			case 0 -> drum.getGain().get(Gain.VOLUME);
			case 1 -> drum.getGain().get(Gain.PAN);
			case 2 -> drum.getEnv().getAttack();
			case 3 -> drum.getEnv().getDecay();
			case 4 -> drum != null ? drum.getPitchKnob() : -1; // convenience: mid freq param is log scale, so allow direct knob control
			case 5 -> drum != null && drum.isSync() ? 100 : 0;
			default -> -1;
		};
	}

	private int getFilter(int idx) {
		return switch (idx) {
			case 0 -> drum.getHzKnob(EqBand.Bass);
			case 1 -> drum.getHzKnob(EqBand.High);
			case 2 -> drum.getResonanceKnob(EqBand.Bass);
			case 3 -> drum.getResonanceKnob(EqBand.High);
			case 4 -> drum.getHzKnob(EqBand.Mid);
			case 5 -> drum.getResonanceKnob(EqBand.Mid);
			case 6 -> drum.getMid().getBandwidth() != 0 ?
					Math.round(((drum.getMid().getBandwidth() - Filters.MIN_BANDWIDTH)
							/ (Filters.MAX_BANDWIDTH - Filters.MIN_BANDWIDTH)) * 100f) : 0;
			default -> -1;
		};
	}


}
