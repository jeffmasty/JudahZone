package net.judah.effects.gui;

import java.awt.Component;
import java.util.ArrayList;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.effects.LFO;
import net.judah.effects.LFO.Target;
import net.judah.mixer.Channel;
import net.judah.util.Constants;
import net.judah.util.JudahKnob;


public class LfoKnobs extends Row implements Widget {

	@Getter protected final LFO lfo;
    @Getter private final ArrayList<Component> controls = new ArrayList<>();

    @Getter private final ArrayList<String> list = new ArrayList<>();
    
    public LfoKnobs(Channel ch) {
    	super(ch);
    	lfo = ch.getLfo();
    	
    	for (LFO.Target t : LFO.Target.values())
    		list.add(t.name());
    	
		controls.add(new FxCombo(this)); // lfo type
		controls.add(new JudahKnob(ch, lfo, LFO.Settings.Min.ordinal(), LFO.Settings.Min.name()));
		controls.add(new JudahKnob(ch, lfo, LFO.Settings.Max.ordinal(), LFO.Settings.Max.name()));
		controls.add(new JudahKnob(ch, lfo, LFO.Settings.MSec.ordinal(), "Time"));
		// TODO  TapTempo time = new TapTempo("time", msec -> { if (msec > 0) {
		//      	lfo.setFrequency((int)msec);
		//          lfoFreq.setValue((int)lfo.getFrequency());
		//          RTLogger.log(this, "LFO Tap Tempo: " + lfo.getFrequency());}});
    }

	@Override
	public void increment(boolean up) {
		int next = lfo.getTarget().ordinal() + (up ? 1 : -1);
		if (next >= LFO.Target.values().length)
			next = 0;
		if (next < 0)
			next = LFO.Target.values().length - 1;
		lfo.setTarget(LFO.Target.values()[next]);
	}

	@Override
	public int getIdx() {
		return lfo.getTarget().ordinal();
	}

	public void knob(int idx, int data2) {
    	
    	switch(idx) {
    	case 0:
    		channel.getLfo().setTarget((Target)Constants.ratio(data2, LFO.Target.values()));
    		break;
    	case 1: 
    		channel.getLfo().setMin(data2);
    		break;
    	case 2: 
    		channel.getLfo().setMax(data2);
    		break;
    	case 3: 
    		channel.getLfo().set(LFO.Settings.MSec.ordinal(), data2);
    		break;
    	default: return;
    	}
    	MainFrame.update(this);
	}
}
