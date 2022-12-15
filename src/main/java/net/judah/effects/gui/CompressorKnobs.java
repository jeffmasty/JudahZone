package net.judah.effects.gui;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.effects.Compressor;
import net.judah.mixer.Channel;
import net.judah.widgets.JudahKnob;

/** EffectsRack Compressor settings */
public class CompressorKnobs extends JPanel {

	@Getter private final ArrayList<Component> controls = new ArrayList<>();
	
	private final Compressor comp; 
	
	public CompressorKnobs(Channel ch) {
		comp = ch.getCompression();
		update();
	}

//	public void knob(int idx, int data2) {
//		switch (idx) {
//		case 0: 
//			comp.set(Ratio.ordinal(), data2);
//			comp.setActive(data2 > 5);
//			break;
//		case 1: // -30 to -1
//			comp.set(Threshold.ordinal(), data2);
//			comp.setActive(data2 > 0);
//			break;
//		case 2:
//			comp.set(Boost.ordinal(), data2);
//			comp.setActive(data2 > 0);
//			break;
//    	case 3:
//			comp.set(Release.ordinal(), data2);
//			comp.set(Attack.ordinal(), data2);
//			comp.setActive(data2 > 0);
//			break;
//    	default: return;
//		}
//		MainFrame.update(this);
//	}
	
	public final void update() {
    	for (Component c : getControls()) 
			if (c instanceof JudahKnob)
				((JudahKnob)c).update();
			else if (c instanceof FxCombo)
				((FxCombo)c).update();
	}

	
}
