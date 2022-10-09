package net.judah.tracker;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.JudahZone;
import net.judah.fluid.FluidSynth;
import net.judah.midi.ProgChange;

public class Patches extends JPanel {

	public Patches() {
		setLayout(new GridLayout(4, 1));
		setBorder(BorderFactory.createTitledBorder("Fluid Insturments"));
		FluidSynth fluid = JudahZone.getFluid();
		for (int i = 0; i < 4; i++) {
			JPanel row = new JPanel();
			row.add(new JLabel("CH " + i));
			ProgChange p = new ProgChange(fluid, i);
			row.add(p);
			add(row);
		}
		
		
	}
	
}
