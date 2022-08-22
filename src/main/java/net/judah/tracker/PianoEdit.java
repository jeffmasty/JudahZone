package net.judah.tracker;

import static net.judah.util.Size.*;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class PianoEdit extends TrackEdit {

	private static final int OCTAVES = 6;
	private final Dimension PIANO_ROLL = new Dimension(WIDTH_SONG - WIDTH_BUTTONS - 30, TABS.height- 10);
	private final JComboBox<Integer> gate = new JComboBox<>();
	JScrollPane scroller;
	
	
	public PianoEdit(PianoTrack track) {
		super(track);
		JPanel pnl = new JPanel(new GridLayout(1, 4));
		JLabel gt = new JLabel("Gate", JLabel.CENTER);
		pnl.add(gt);
		for (int i = 1; i < track.getSteps(); i++)
            gate.addItem(i);
		gate.setSelectedItem(track.getGate());
		gate.addActionListener(this);
		pnl.add(gate);
		JLabel oc = new JLabel("Octave", JLabel.CENTER);
		for (int i = 1; i <= OCTAVES; i++)
		pnl.add(oc);
		pnl.add(new JComboBox<String>());
		
		buttons.add(pnl);
		
        scroller = new JScrollPane();
        if (track.getCurrent() != null)
        	scroller.setViewportView(track.getCurrent().getTable());
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
		if (e.getSource() == gate)
			((PianoTrack)track).setGate((int)gate.getSelectedItem());
	}

	@Override
	public void update() {
		track.getCurrent().update();
	}

	@Override
	public void setPattern(int idx) {
		super.setPattern(idx);
		Pattern p = track.getCurrent();
		p.getTable().setPreferredScrollableViewportSize(PIANO_ROLL);
		p.update();
		scroller.setViewportView(p.getTable());
	}

}
