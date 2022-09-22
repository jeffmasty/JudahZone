package net.judah.synth;
import java.awt.GridLayout;

import javax.swing.JPanel;

import net.judah.JudahZone;

public class SynthEngines extends JPanel {

	public static final String NAME = "Synths";
	
	static SynthEngines instance;
	
    private SynthEngines() {
    	instance = this;
    	setName(NAME);
        JPanel row = new JPanel(new GridLayout(1, 2));
        row.add(JudahZone.getSynth().getView());
        row.add(JudahZone.getSynth2().getView());

        JPanel outer = new JPanel();
        outer.add(row);
        add(outer);
    }

    public static SynthEngines getInstance() {
    	if (instance == null)
    		instance = new SynthEngines();
    	return instance;
    }
    
}
