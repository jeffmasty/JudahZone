package net.judah.samples;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class SamplerView extends JPanel {

	private final Sampler sampler;
	
	public SamplerView(Sampler sampler) {
		this.sampler = sampler;
		setBorder(BorderFactory.createTitledBorder("Samples"));
		JPanel loops = new JPanel(new GridLayout(1, 4, 3, 3));
		JPanel oneShots = new JPanel(new GridLayout(1, 4, 3, 3));
		for (int i = 0; i < 4; i++)
			loops.add(sampler.get(i).getPad());
		for (int i = 4; i < 8; i++)
			oneShots.add(sampler.get(i).getPad());

		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setOpaque(true);
		add(loops);
		add(oneShots);
		
	}
	
	public void update(Sample samp) {
		for (Sample s : sampler)
			if (s == samp) 
				s.getPad().update();
		repaint();
	}
	
}
