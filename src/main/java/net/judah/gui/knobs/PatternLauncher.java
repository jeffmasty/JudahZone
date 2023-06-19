package net.judah.gui.knobs;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JPanel;

import net.judah.gui.Size;
import net.judah.seq.MidiTrack;

public class PatternLauncher extends JPanel {
	private static final int SIZE = Size.STD_HEIGHT;
	private static final int UNITS = 8;
	
//	class Button extends JLabel {
//		Button(String lbl, ActionListener action) {
//			super(lbl);
//			addMouseListener(new MouseAdapter() {
//				@Override public void mouseClicked(MouseEvent e) {
//					action.actionPerformed(null);
//		}});}
//	}
//	private final Button left, right;
	private final MidiTrack track;
	private final ArrayList<TrackPattern> patterns = new ArrayList<>();
	private final JPanel holder = new JPanel();
	
	public PatternLauncher(MidiTrack track) {
		setLayout(new FlowLayout(FlowLayout.LEFT, 1, 0));
		this.track = track;
		
//		left = new Button("<", e->position(false));
//		right = new Button(">", e->position(true));
		Dimension holderSz = new Dimension(SIZE * UNITS, SIZE);
		
		holder.setLayout(new GridLayout(1, UNITS, 1, 0));
		holder.setMaximumSize(holderSz);
		holder.setPreferredSize(holderSz);
		setPreferredSize(new Dimension(holderSz.width + 2 * SIZE, SIZE));
		setMaximumSize(new Dimension(holderSz.width + 2 * SIZE, SIZE));
		setOpaque(true);
		fill();
//		add(left);
		add(holder);
//		add(right);
	}

//	private void position(boolean right) {	}
	
	public void update() {
		
		if (frames() != patterns.size())
			fill();
		for (TrackPattern pattern : patterns)
			pattern.update();
//		left.setVisible(track.bars() > 6);
//		right.setVisible(track.bars() > 6);
//		if (track.size() > UNITS) {
//			left.setVisible(true);
//			right.setVisible(true);
//		}
//		left.setVisible(track.size() > UNITS);
//		right.setVisible(track.size() > UNITS);
//		left.setEnabled(position > 0);
//		right.setEnabled(track.size() > UNITS && position < track.size() - UNITS);
//		
//		Bar current = track.get(track.getCurrent());
//		for (int i = 0; i < holder.getComponentCount(); i++) {
//			BarStool btn = (BarStool)holder.getComponent(i);
//			btn.setBackground(btn.getBar() == current ? Pastels.GREEN : Pastels.MY_GRAY);
//		}
	}

	int frames() {
		return (int) Math.ceil(track.bars() / 2);
	}
	
	public void fill() {
		holder.removeAll();
		patterns.clear();
		for (int i = 0; i < frames(); i++) 
			patterns.add((TrackPattern)holder.add(new TrackPattern(track, i)));
		update();
		invalidate();
	}
	
}
