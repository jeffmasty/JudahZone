package net.judah.synth;
import java.awt.GridLayout;

import javax.swing.JPanel;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.fluid.FluidSynth;
import net.judah.samples.SamplerView;

public class SynthEngines extends JPanel {

	public static final String NAME = "Synth";
	@Setter @Getter private static SynthView current;
	private static SynthEngines instance;
	@Getter private final SynthView synth1, synth2; 
	@Getter private final SamplerView samples;
	
    private SynthEngines() {
    	instance = this;
    	setOpaque(true);
    	setName(NAME);
        JPanel row = new JPanel(new GridLayout(1, 2));
        synth2 = new SynthView(JudahZone.getSynth2());
        synth1 = new SynthView(JudahZone.getSynth1());
        row.add(synth1);
        row.add(synth2);
        JPanel outer = new JPanel();
        outer.add(row);
        add(outer);
        
        samples = new SamplerView(JudahZone.getSampler());
        JPanel bottom = new JPanel();
        bottom.add(samples);
        FluidSynth fluid = JudahZone.getFluid();
        bottom.add(fluid.getChannels().getGui(fluid));
        add(bottom);

    }

    public static SynthEngines getInstance() {
    	if (instance == null)
    		instance = new SynthEngines();
    	return instance;
    }

	public static void flip() {
		if (instance == null)
			return;
		if (current == instance.synth1)
			current = instance.synth2;
		else 
			current = instance.synth1;
	}
    
}
