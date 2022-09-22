package net.judah.samples;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class SamplerView extends JPanel {

	public SamplerView(Sampler sampler) {
		setBorder(BorderFactory.createTitledBorder("Samples"));
		JPanel loops = new JPanel(new GridLayout(1, 4, 3, 3));
		JPanel oneShots = new JPanel(new GridLayout(1, 4, 3, 3));

		for (int i = 0; i < 4; i++)
			loops.add(sampler.get(i).getPad());
		for (int i = 4; i < 8; i++)
			oneShots.add(sampler.get(i).getPad());

		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.PAGE_AXIS));
		JComboBox<String> presets = new JComboBox<String>();
		
		presets.addItem("House");
		buttons.add(presets);
		buttons.add(new JButton("Save"));
		add(buttons);
		add(loops);
		add(oneShots);
		
	}
	
	
	
}
