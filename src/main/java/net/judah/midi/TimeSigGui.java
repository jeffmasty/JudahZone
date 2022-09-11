package net.judah.midi;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.controllers.MPK;
import net.judah.tracker.Tracker;

public class TimeSigGui extends JPanel {

	public static final int[] DIVS = new int[] {2,3,4,6,8};
	
	private final JComboBox<Integer> steps = new JComboBox<>();
    private final JComboBox<Integer> div = new JComboBox<>();
    private final JudahClock clock;
    
	TimeSigGui(JudahClock clock, JudahMidi midi) {
		this.clock = clock;
		for (int i : TimeSigGui.DIVS)
            div.addItem(i);
		for (int i = 2; i <= 32; i++)
			steps.addItem(i);
		update();
		div.addActionListener(e -> clock.setSubdivision((int)div.getSelectedItem()));
		steps.addActionListener(e -> clock.setSteps((int)steps.getSelectedItem()));

		add(MPK.getLabel());
		add(Tracker.getLabel());
		add(steps);
		add(new JLabel("steps/div", JLabel.CENTER));
		add(div);
	}
	
	public void update() {
		if (clock.getSteps() != (int)steps.getSelectedItem())
			steps.setSelectedItem(clock.getSteps());
		if (clock.getSubdivision() != (int)div.getSelectedItem())
			div.setSelectedItem(clock.getSubdivision());
	}
	
}
