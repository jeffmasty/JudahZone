package net.judah.effects.gui;

import java.awt.Component;
import java.util.ArrayList;

import lombok.Getter;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPK;
import net.judah.effects.Chorus;
import net.judah.effects.Compression;
import net.judah.effects.CutFilter;
import net.judah.effects.Delay;
import net.judah.effects.EQ;
import net.judah.effects.api.Gain;
import net.judah.effects.api.Reverb;
import net.judah.mixer.Channel;
import net.judah.util.JudahKnob;

public class RowKnobs extends Row {

	@Getter private final ArrayList<Component> controls = new ArrayList<>();
	private final int row;
	
	public RowKnobs(final Channel ch, KnobMode mode, int idx) {
		super(ch, mode);
		row = idx;
		switch (idx) {
		case 0 : 
			controls.add(new JudahKnob(ch, ch.getReverb(), Reverb.Settings.Wet.ordinal(), Reverb.Settings.Wet.name()));
			controls.add(new JudahKnob(ch, ch.getReverb(), Reverb.Settings.Room.ordinal(), Reverb.Settings.Room.name()));
			controls.add(new JudahKnob(ch, ch.getReverb(), Reverb.Settings.Damp.ordinal(), Reverb.Settings.Damp.name()));
			controls.add(new JudahKnob(ch, ch.getGain(), Gain.VOLUME, ""));
			break;
		case 1: 
			controls.add(new JudahKnob(ch, ch.getChorus(), Chorus.Settings.Rate.ordinal(), Chorus.Settings.Rate.name()));
			controls.add(new JudahKnob(ch, ch.getChorus(), Chorus.Settings.Depth.ordinal(), Chorus.Settings.Depth.name()));
			controls.add(new JudahKnob(ch, ch.getChorus(), Chorus.Settings.Feedback.ordinal(), "F/B"));
			controls.add(new JudahKnob(ch, ch.getCutFilter(), CutFilter.Settings.Frequency.ordinal(), "Hz."));
			break;
		case 2: 
			controls.add(new JudahKnob(ch, ch.getEq(), EQ.EqBand.Bass.ordinal(), EQ.EqBand.Bass.name()));
			controls.add(new JudahKnob(ch, ch.getEq(), EQ.EqBand.Mid.ordinal(), EQ.EqBand.Mid.name()));
			controls.add(new JudahKnob(ch, ch.getEq(), EQ.EqBand.High.ordinal(), EQ.EqBand.High.name()));
			controls.add(new JudahKnob(ch, ch.getDelay(), Delay.Settings.DelayTime.ordinal(), "Time"));
			break;
		case 3: 
			controls.add(new JudahKnob(ch, ch.getOverdrive(), 0, "Gain"));
			controls.add(new JudahKnob(ch, ch.getGain(), Gain.PAN, ""));
			controls.add(new JudahKnob(ch, ch.getCompression(), Compression.Settings.Threshold.ordinal(), "Thold"));
			controls.add(new JudahKnob(ch, ch.getDelay(), Delay.Settings.Feedback.ordinal(), "F/B"));
			break;
		}
		update();
		
	}

	@Override
	public void update() {
		for (Component c : controls) 
			((JudahKnob)c).update();
		
		boolean on = (row == 0 || row == 1) ? 
				KnobMode.Effects1 == MPK.getMode() : 
				KnobMode.Effects2 == MPK.getMode(); 
		for (Component c : controls) 
			((JudahKnob)c).setOnMode(on);
	}

}
