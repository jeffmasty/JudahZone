package net.judah.gui.fx;

import net.judah.fx.Chorus;
import net.judah.fx.Delay;
import net.judah.fx.Gain;
import net.judah.fx.Overdrive;
import net.judah.fx.Reverb;
import net.judah.gui.widgets.FxKnob;
import net.judah.mixer.Channel;
import net.judah.synth.taco.MonoFilter;
import net.judah.util.RTLogger;

public class RowKnobs extends Row {

	public RowKnobs(Channel ch, EQPlus eq) {
		super(ch);
		controls.add(eq.getLeft());
		controls.add(eq.getCenter());
		controls.add(eq.getRight());
		controls.add(new FxKnob(ch, ch.getGain(), Gain.PAN, ""));
	}

	public RowKnobs(final Channel ch, int idx) {
		super(ch);
		switch (idx) {
		case 0 :
			controls.add(new FxKnob(ch, Reverb.Settings.Wet.ordinal(), Reverb.Settings.Wet.name()));
			controls.add(new FxKnob(ch, Reverb.Settings.Room.ordinal(), Reverb.Settings.Room.name(), Reverb.Settings.Damp.ordinal()));
			controls.add(new FxKnob(ch, ch.getDelay(), Delay.Settings.Feedback.ordinal(), "F/B", Delay.Settings.Type.ordinal()));
			controls.add(new FxKnob(ch, ch.getDelay(), Delay.Settings.DelayTime.ordinal(), "Time", Delay.Settings.Sync.ordinal()));
			break;
		case 1:
			controls.add(new FxKnob(ch, ch.getOverdrive(), 0, "Gain", Overdrive.Settings.Clipping.ordinal()));
			controls.add(new FxKnob(ch, ch.getChorus(), Chorus.Settings.Depth.ordinal(), Chorus.Settings.Depth.name(), Chorus.Settings.Phase.ordinal()));
			controls.add(new FxKnob(ch, ch.getChorus(), Chorus.Settings.Feedback.ordinal(), "F/B", Chorus.Settings.Type.ordinal()));
			controls.add(new FxKnob(ch, ch.getChorus(), Chorus.Settings.Rate.ordinal(), Chorus.Settings.Rate.name(), Chorus.Settings.Sync.ordinal(), true));
			break;

		// case 2: EQKnobs

		case 3:
			controls.add(new PresetsBtns(ch));
			controls.add(new FxKnob(ch, ch.getLoCut(), MonoFilter.Settings.Frequency.ordinal(), "Hz."));
			controls.add(new FxKnob(ch, ch.getHiCut(), MonoFilter.Settings.Frequency.ordinal(), "Hz.", true));
			controls.add(new FxKnob(ch, ch.getGain(), Gain.VOLUME, ""));
			break;
		default:
			RTLogger.warn(this, new Exception(idx + " what? " + ch));
		}
		update();

	}

}
