package net.judah.gui.fx;

import net.judah.JudahZone;
import net.judah.fx.*;
import net.judah.gui.widgets.FxKnob;
import net.judah.mixer.Channel;
import net.judah.util.RTLogger;

public class RowKnobs extends Row {

	public RowKnobs(final Channel ch, int idx) {
		super(ch);
		switch (idx) {
		case 0 : 
			controls.add(new FxKnob(ch, Reverb.Settings.Wet.ordinal(), Reverb.Settings.Wet.name()));
			controls.add(new FxKnob(ch, Reverb.Settings.Room.ordinal(), Reverb.Settings.Room.name()));
			controls.add(new FxKnob(ch, ch.getDelay(), Delay.Settings.Feedback.ordinal(), "F/B"));
			controls.add(new FxKnob(ch, ch.getDelay(), Delay.Settings.DelayTime.ordinal(), "Time"));
			break;
		case 1: 
			controls.add(new FxKnob(ch, ch.getOverdrive(), 0, "Gain"));
			controls.add(new FxKnob(ch, ch.getChorus(), Chorus.Settings.Depth.ordinal(), Chorus.Settings.Depth.name()));
			controls.add(new FxKnob(ch, ch.getChorus(), Chorus.Settings.Feedback.ordinal(), "F/B"));
			controls.add(new FxKnob(ch, ch.getChorus(), Chorus.Settings.Rate.ordinal(), Chorus.Settings.Rate.name()));
			break;
		case 2: 
			controls.add(new FxKnob(ch, ch.getEq(), EQ.EqBand.Bass.ordinal(), EQ.EqBand.Bass.name()));
			controls.add(new FxKnob(ch, ch.getEq(), EQ.EqBand.Mid.ordinal(), EQ.EqBand.Mid.name()));
			controls.add(new FxKnob(ch, ch.getEq(), EQ.EqBand.High.ordinal(), EQ.EqBand.High.name()));
			controls.add(new FxKnob(ch, ch.getGain(), Gain.VOLUME, ""));
			break;
		case 3: 
			controls.add(new PresetsBtns(ch, JudahZone.getLooper()));
			controls.add(new FxKnob(ch, ch.getFilter1(), Filter.Settings.Frequency.ordinal(), "Hz."));
			controls.add(new FxKnob(ch, ch.getFilter2(), Filter.Settings.Frequency.ordinal(), "Hz."));
			controls.add(new FxKnob(ch, ch.getGain(), Gain.PAN, ""));			
			break;
		default:
			RTLogger.warn(this, new Exception(idx + " what? " + ch));
		}
		update();
		
	}

}
