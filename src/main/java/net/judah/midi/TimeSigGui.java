package net.judah.midi;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.*;

import net.judah.controllers.MPKmini;
import net.judah.tracker.JudahBeatz;

public class TimeSigGui extends JPanel {

	public static final int[] DIVS = new int[] {2,3,4,6,8};
	
	private final JComboBox<Integer> steps = new JComboBox<>();
    private final JComboBox<Integer> div = new JComboBox<>();
    private final JudahClock clock;
    
    public TimeSigGui(JudahClock clock, JudahMidi midi, JudahBeatz t) {
		setBorder(BorderFactory.createLineBorder(Color.black));
		this.clock = clock;
		for (int i : TimeSigGui.DIVS)
            div.addItem(i);
		for (int i = 2; i <= 32; i++)
			steps.addItem(i);
		update();
		div.addActionListener(e -> clock.setSubdivision((int)div.getSelectedItem()));
		steps.addActionListener(e -> clock.setSteps((int)steps.getSelectedItem()));
		Dimension sz = new Dimension(50, 30);
		steps.setMaximumSize(sz);
		div.setMaximumSize(sz);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		MPKmini.getLabel().setOpaque(true);
		MPKmini.getLabel().setBorder(BorderFactory.createRaisedSoftBevelBorder());
		
		add(MPKmini.getLabel());
		add(Box.createHorizontalGlue());
		add(t.getLabel());
		add(Box.createHorizontalGlue());
		add(steps);
		add(new JLabel("stp/dv", JLabel.CENTER));
		add(div);
	}
	
	public void update() {
		if (clock.getSteps() != (int)steps.getSelectedItem())
			steps.setSelectedItem(clock.getSteps());
		if (clock.getSubdivision() != (int)div.getSelectedItem())
			div.setSelectedItem(clock.getSubdivision());
	}
	
}
