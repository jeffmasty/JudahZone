package net.judah.gui.player;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.gui.Size;
import net.judah.seq.Bar;
import net.judah.seq.MidiTrack;

public class BarRoom extends JPanel {
	private static final int SIZE = Size.STD_HEIGHT;
	private static final int UNITS = 8;
	
	class Button extends JLabel {
		Button(String lbl, ActionListener action) {
			super(lbl);
			addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					action.actionPerformed(null);
		}});}
		
	}
	private final Button left, right;
	private final MidiTrack track;
	private final JPanel holder = new JPanel();
	private int position;
	
	public BarRoom(MidiTrack track) {
		setLayout(new FlowLayout(FlowLayout.LEFT, 1, 0));
		this.track = track;
		
		left = new Button("<", e->position(false));
		right = new Button(">", e->position(true));
		Dimension holderSz = new Dimension(SIZE * UNITS, SIZE);
		
		holder.setLayout(new GridLayout(1, UNITS, 1, 0));
		holder.setMaximumSize(holderSz);
		holder.setPreferredSize(holderSz);
		setPreferredSize(new Dimension(holderSz.width + 2 * SIZE, SIZE));
		setMaximumSize(new Dimension(holderSz.width + 2 * SIZE, SIZE));
		setOpaque(true);
		fill();
		add(left);
		add(holder);
		add(right);
	}

	private void position(boolean right) {
		
	}
	
	public void update() {
		for (int i = 0; i < holder.getComponentCount(); i++) {
			if (holder.getComponent(i) instanceof BarStool)
				((BarStool)holder.getComponent(i)).update();
		}
		left.setVisible(track.size() > 6);
		right.setVisible(track.size() > 6);
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

	public void fill() {
		holder.removeAll();
		for (Bar b : track) 
			holder.add(new BarStool(track, b));
		update();
		invalidate();
	}
	
}
