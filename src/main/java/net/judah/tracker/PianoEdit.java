package net.judah.tracker;

import static net.judah.util.Size.*;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.judah.util.RTLogger;

public class PianoEdit extends TrackEdit {

	private static final int OCTAVES = 6;
	private final PianoTrack pianoTrack;
	private final Dimension PIANO_ROLL = new Dimension(WIDTH_SONG - WIDTH_BUTTONS - 30, TABS.height- 10);
	private final JComboBox<Integer> gate = new JComboBox<>();
	private final JComboBox<Integer> octave = new JComboBox<>();
	JScrollPane scroller;
	
	
	public PianoEdit(PianoTrack track, int viewOctave) {
		super(track);
		pianoTrack = track;
		JPanel pnl = new JPanel(new GridLayout(1, 4));
		JLabel gt = new JLabel("Gate", JLabel.CENTER);
		pnl.add(gt);
		for (int i = 1; i < track.getSteps(); i++)
            gate.addItem(i);
		gate.setSelectedItem(track.getGate());
		gate.addActionListener(this);
		pnl.add(gate);
		JLabel oc = new JLabel("Octave", JLabel.CENTER);
		for (int i = 0; i <= OCTAVES; i++)
			octave.addItem(i);
		pnl.add(oc);
		pnl.add(octave);
		buttons.add(pnl);
		
        scroller = new JScrollPane();
        if (track.getCurrent() != null)
        	scroller.setViewportView(track.getCurrent().getTable());
        scroller.setVisible(true);
        add(scroller);

        octave.addActionListener(this);
		octave.setSelectedItem(viewOctave);
	}

	@Override
	public void step(int step) {
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (disable) return;
		if (super.handled(e))
			return;
		if (e.getSource() == gate)
			pianoTrack.setGate((int)gate.getSelectedItem());
		else if (e.getSource() == octave) {
			adjustScroller();
		}
	}

	@Override
	public void update() {
		track.getCurrent().update();
	}

	@Override
	public void setPattern(int idx) {
		Pattern p = track.get(idx);
		p.getTable().setPreferredScrollableViewportSize(PIANO_ROLL);
		refresh(p);
		super.setPattern(idx);
	}

	private void adjustScroller() {
		int y = (1 + Math.abs((int)octave.getSelectedItem() - OCTAVES))
				* (int)(scroller.getSize().height / (float)OCTAVES);
		scroller.getViewport().setViewPosition(new Point(0, y));
		scroller.invalidate();
	}
	
	public void refresh(Pattern p) {
		if (track.getDiv() != (int)div.getSelectedItem() || 
				track.getSteps() != (int)steps.getSelectedItem())
			track.getCurrent().getTable().setModel(track.getCurrent());
		p.update();
		try {
			if (scroller.getViewport().getView() != p.getTable())
				scroller.setViewportView(p.getTable());
		} catch (Throwable t) {
			RTLogger.log(this, "viewport hiccup " + track.getName() + ":" + p);
		}
		adjustScroller();
	}

}
