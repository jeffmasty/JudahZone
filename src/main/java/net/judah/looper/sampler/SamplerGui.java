package net.judah.looper.sampler;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JDesktopPane;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.JudahZone;

public class SamplerGui extends JDesktopPane {

	static SamplerGui instance;
	
	private final Sampler sampler = JudahZone.getSampler();
    private final JPanel pads = new JPanel();

    private SamplerGui() {
    	instance = this;
    	setName("Sampler");
    	JPanel loops = new JPanel(new GridLayout(2, 2, 3, 3));
		JPanel oneShots = new JPanel(new GridLayout(2, 2, 3, 3));

		for (int i = 0; i < 4; i++)
			loops.add(sampler.get(i).getPad());
		for (int i = 4; i < 8; i++)
			oneShots.add(sampler.get(i).getPad());
        
        pads.setLayout(new BoxLayout(pads, BoxLayout.LINE_AXIS));
		pads.add(loops);
		pads.add(oneShots);
		
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        JPanel row = new JPanel(new GridLayout(1, 2));
        row.add(pads);
        row.add(JudahZone.getSynth().getView());
        add(row);
        row = new JPanel(new GridLayout(1, 2));
        row.add(new JLabel(" ")); // beatz
        row.add(new JLabel(" ")); // scope
        add(row);
    }

    public static SamplerGui getInstance() {
    	if (instance == null)
    		instance = new SamplerGui();
    	return instance;
    }
    
}
