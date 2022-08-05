package net.judah.tracker.todo;

import static net.judah.util.Size.*;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.judah.tracker.Track;
import net.judah.tracker.TrackEdit;

public class PianoEdit extends TrackEdit {

	private static final int OCTAVES = 6;
	private final Dimension PIANO_ROLL = new Dimension(WIDTH_SONG - WIDTH_BUTTONS - 30, TABS.height- 10);
	private final JudahPiano roll;
	private final JComboBox<Integer> gate = new JComboBox<>();
	
	public PianoEdit(Track track) {
		super(track);
		JPanel pnl = new JPanel(new GridLayout(1, 4));
		JLabel gt = new JLabel("Gate", JLabel.CENTER);
		pnl.add(gt);
		for (int i = 0; i < track.getSteps(); i++)
            gate.addItem(i);

		pnl.add(gate);
		JLabel oc = new JLabel("Octave", JLabel.CENTER);
		for (int i = 1; i <= OCTAVES; i++)
		pnl.add(oc);
		pnl.add(new JComboBox<String>());
		
		buttons.add(pnl);
		
		roll = new JudahPiano(track, PIANO_ROLL);

        JScrollPane scroller = new JScrollPane(roll);
        scroller.setVisible(true);
        add(scroller);
		
	}

	@Override
	public void step(int step) {
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (super.handled(e))
			return;
	}

	@Override
	public void update() {
		roll.repaint();
	}

}
