package net.judah.effects.gui;

import net.judah.api.JudahException;
import net.judah.effects.Chorus;
import net.judah.effects.CutFilter;
import net.judah.effects.Delay;
import net.judah.effects.EQ;
import net.judah.effects.api.Gain;
import net.judah.effects.api.Reverb;
import net.judah.mixer.Channel;
import net.judah.util.RTLogger;
import net.judah.widgets.JudahKnob;

public class RowKnobs extends Row {

	public RowKnobs(final Channel ch, int idx) {
		super(ch);
		switch (idx) {
		case 0 : 
			controls.add(new JudahKnob(ch, ch.getReverb(), Reverb.Settings.Wet.ordinal(), Reverb.Settings.Wet.name()));
			controls.add(new JudahKnob(ch, ch.getReverb(), Reverb.Settings.Room.ordinal(), Reverb.Settings.Room.name()));
			controls.add(new JudahKnob(ch, ch.getDelay(), Delay.Settings.Feedback.ordinal(), "F/B"));
			controls.add(new JudahKnob(ch, ch.getDelay(), Delay.Settings.DelayTime.ordinal(), "Time"));
			break;
		case 1: 
			controls.add(new JudahKnob(ch, ch.getChorus(), Chorus.Settings.Rate.ordinal(), Chorus.Settings.Rate.name()));
			controls.add(new JudahKnob(ch, ch.getChorus(), Chorus.Settings.Depth.ordinal(), Chorus.Settings.Depth.name()));
			controls.add(new JudahKnob(ch, ch.getChorus(), Chorus.Settings.Feedback.ordinal(), "F/B"));
			controls.add(new JudahKnob(ch, ch.getOverdrive(), 0, "Gain"));
			break;
		case 2: 
			controls.add(new JudahKnob(ch, ch.getEq(), EQ.EqBand.Bass.ordinal(), EQ.EqBand.Bass.name()));
			controls.add(new JudahKnob(ch, ch.getEq(), EQ.EqBand.Mid.ordinal(), EQ.EqBand.Mid.name()));
			controls.add(new JudahKnob(ch, ch.getEq(), EQ.EqBand.High.ordinal(), EQ.EqBand.High.name()));
			controls.add(new JudahKnob(ch, ch.getGain(), Gain.VOLUME, ""));
			break;
		case 3: 
			controls.add(new FxCombo(ch.getPresets()));
			controls.add(new JudahKnob(ch, ch.getCutFilter(), CutFilter.Settings.Frequency.ordinal(), "Hz."));
			controls.add(new JudahKnob(ch, ch.getHiCut(), CutFilter.Settings.Frequency.ordinal(), "Hz."));
			controls.add(new JudahKnob(ch, ch.getGain(), Gain.PAN, ""));
			break;
		default:
			RTLogger.warn(this, new JudahException(idx + " what? " + ch));
		}
		update();
		
	}

}
