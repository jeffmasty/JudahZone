package net.judah.synth;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.controllers.KnobMode;
import net.judah.controllers.Knobs;
import net.judah.controllers.MPKmini;
import net.judah.fluid.FluidSynth;
import net.judah.samples.SamplerView;

@Getter
public class SynthEngines extends JPanel implements Knobs {

	public static final String NAME = "Synth";
	@Setter @Getter private static SynthView current;
	private static SynthEngines instance;
	private final SynthView synth1, synth2; 
	private final SamplerView samples;
	private KnobMode knobMode = KnobMode.Synth1;
	
    public static SynthEngines getInstance() {
    	if (instance == null)
    		instance = new SynthEngines();
    	return instance;
    }

    private SynthEngines() {
    	instance = this;
    	setOpaque(true);
    	setName(NAME);
		JPanel row = new JPanel(/* new GridLayout(1, 2) */);
        synth2 = new SynthView(JudahZone.getSynth2());
        synth1 = new SynthView(JudahZone.getSynth1());
        row.add(synth1);
        row.add(new JLabel("   "));
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

	public static void flip() {
		if (instance == null)
			return;
		if (current == instance.synth2)
			current = instance.synth1;
		else 
			current = instance.synth2;
		MPKmini.setMode(current.getKnobMode());
	}
    
}
