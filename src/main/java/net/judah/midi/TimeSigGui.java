package net.judah.midi;

import java.awt.Dimension;

import javax.swing.*;

import net.judah.controllers.MPKmini;
import net.judah.tracker.ActiveTracks;
import net.judah.util.Constants;
import net.judah.util.Pastels;
import net.judah.util.Size;

public class TimeSigGui extends JPanel {

	public static final int[] DIVS = new int[] {2,3,4,6,8};
	
	private final JComboBox<Integer> steps = new JComboBox<>();
    private final JComboBox<Integer> div = new JComboBox<>();
    private final JudahClock clock;
    
    public TimeSigGui(JudahClock clock, ActiveTracks actives, JLabel currentTrack) {
		setBorder(BorderFactory.createLineBorder(Pastels.MY_GRAY));
		setPreferredSize(new Dimension(Size.WIDTH_CONTROLS + 20, 28));
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
		JLabel mpk = MPKmini.getLabel();
        mpk.setOpaque(true);
        mpk.setFont(Constants.Gui.BOLD13);
        
        add(Box.createHorizontalStrut(20));
        add(mpk);
        add(Box.createHorizontalStrut(10));
        add(actives);
		add(Box.createHorizontalGlue());
        add(currentTrack);
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
