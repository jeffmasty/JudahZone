package net.judah.effects.gui;

import static net.judah.effects.Compressor.Settings.*;

import java.awt.Component;
import java.util.ArrayList;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.effects.Compressor;
import net.judah.mixer.Channel;
import net.judah.util.JudahKnob;

/** EffectsRack Bottom Row. Preset drop down, Compressor settings */
public class CompressorKnobs extends Row {

	@Getter private final ArrayList<Component> controls = new ArrayList<>();
	
	private final Compressor comp; 
	
	public CompressorKnobs(Channel ch) {
		super(ch);
		comp = ch.getCompression();
		controls.add(new JudahKnob(ch, comp, Threshold.ordinal(), "T/Hold"));
		controls.add(new JudahKnob(ch, comp, Ratio.ordinal(), "Ratio"));
		controls.add(new JudahKnob(ch, comp, Attack.ordinal(), "Atk"));
		controls.add(new JudahKnob(ch, comp, Release.ordinal(), "Rel"));
		update();
	}

	public void knob(int idx, int data2) {
		switch (idx) {
		case 4: // -30 to -1
			comp.set(Threshold.ordinal(), data2);
			comp.setActive(data2 > 0);
			break;
		case 5: 
			comp.set(Ratio.ordinal(), data2);
			comp.setActive(data2 > 5);
			break;
		case 6:
			comp.set(Attack.ordinal(), data2);
			comp.setActive(data2 > 0);
			break;
    	case 7:
			comp.set(Release.ordinal(), data2);
			comp.setActive(data2 > 0);
			break;
    	default: return;
		}
		MainFrame.update(this);
	}
	
}
