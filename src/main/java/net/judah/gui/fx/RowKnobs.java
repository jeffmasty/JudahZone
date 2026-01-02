package net.judah.gui.fx;

import judahzone.fx.Chorus;
import judahzone.fx.Delay;
import judahzone.fx.Gain;
import judahzone.fx.MonoFilter;
import judahzone.fx.Overdrive;
import judahzone.util.RTLogger;
import net.judah.channel.Channel;
import net.judah.gui.widgets.FxKnob;


// EffectsRack construction helper
public class RowKnobs extends Row {


	public RowKnobs(Channel ch, ReverbPlus reverb) {
		super(ch);

		add(reverb.getLeft());
		add(reverb.getRight());

		add(new FxKnob(ch, ch.getDelay(), Delay.Settings.Feedback.ordinal(),
				"F/B", Delay.Settings.Type.ordinal()));
		add(new FxKnob(ch, ch.getDelay(), Delay.Settings.DelayTime.ordinal(),
				"Time", Delay.Settings.Sync.ordinal()));

	}

	public RowKnobs(final Channel ch, int idx) {
		super(ch);
		switch (idx) {
//		case 0 :
//			add(new FxKnob(ch, Reverb.Settings.Wet.ordinal(), Reverb.Settings.Wet.name()));
//			add(new FxKnob(ch, Reverb.Settings.Room.ordinal(),
//					Reverb.Settings.Room.name(), Reverb.Settings.Damp.ordinal()));
//
//			add(new FxKnob(ch, ch.getDelay(), Delay.Settings.Feedback.ordinal(),
//					"F/B", Delay.Settings.Type.ordinal()));
//			add(new FxKnob(ch, ch.getDelay(), Delay.Settings.DelayTime.ordinal(),
//					"Time", Delay.Settings.Sync.ordinal()));
//			break;
		case 1:
			add(new FxKnob(ch, ch.getOverdrive(), Overdrive.Settings.Drive.ordinal(),
					"Gain", Overdrive.Settings.Clipping.ordinal()));
			add(new FxKnob(ch, ch.getChorus(), Chorus.Settings.Depth.ordinal(),
					Chorus.Settings.Depth.name(), Chorus.Settings.Phase.ordinal()));
			add(new FxKnob(ch, ch.getChorus(), Chorus.Settings.Feedback.ordinal(),
					"F/B", Chorus.Settings.Type.ordinal()));
			add(new FxKnob(ch, ch.getChorus(), Chorus.Settings.Rate.ordinal(),
					Chorus.Settings.Rate.name(), Chorus.Settings.Sync.ordinal(), true));
			break;

		// case 2: EQKnobs

		case 3:
			add(new PresetsBtns(ch));
			add(new FxKnob(ch, ch.getLoCut(), MonoFilter.Settings.Frequency.ordinal(), "Hz."));
			add(new FxKnob(ch, ch.getHiCut(), MonoFilter.Settings.Frequency.ordinal(), "Hz.", true));
			add(new FxKnob(ch, ch.getGain(), Gain.VOLUME, ""));
			break;
		default:
			RTLogger.warn(this, new Exception(idx + " what? " + ch));
		}
		update();

	}

}
