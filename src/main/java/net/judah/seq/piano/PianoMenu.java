package net.judah.seq.piano;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import net.judah.JudahZone;
import net.judah.gui.Actionable;
import net.judah.gui.Detached.Floating;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.settable.ModeCombo;
import net.judah.gui.widgets.Arrow;
import net.judah.gui.widgets.GateCombo;
import net.judah.gui.widgets.TrackVol;
import net.judah.seq.Duration;
import net.judah.seq.Transpose;
import net.judah.seq.arp.Arp;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.TrackMenu;

public class PianoMenu extends TrackMenu implements Floating {

	private final ButtonGroup mode = new ButtonGroup();

	public PianoMenu(PianoView view, Piano grid) {
		super(grid);
		PianoTrack t = (PianoTrack) track;
		// additional non-drum stuff
		add(new ModeCombo(t));
		zoomMenu(view);
		arpMenu(file, t);
		tools.add(new Actionable("Remap...", e->new Transpose(grid)));
		edit.add(new Actionable("Duration...", e->new Duration(view.getGrid())));

		add(Box.createHorizontalGlue());
		add(new JLabel("Velocity "));
		add(new TrackVol(track));
		add(new JLabel(" Gate"));
		add(Gui.resize(new GateCombo(track), Size.SMALLER_COMBO));
		add(Box.createHorizontalGlue());
	}

	private void zoomMenu(PianoView view) {
		JPanel zoom = new JPanel();
		zoom.setLayout(new BoxLayout(zoom, BoxLayout.LINE_AXIS));
		zoom.setBorder(Gui.SUBTLE);
		zoom.add(Box.createHorizontalStrut(4));
		zoom.add(new Arrow(Arrow.WEST, e->view.tonic(false)));
		zoom.add(new JLabel("Octs"));
		zoom.add(new Octaves(view));
		zoom.add(new Arrow(Arrow.EAST, e->view.tonic(true)));
		zoom.add(Box.createHorizontalStrut(4));
		add(zoom);
	}

	private void arpMenu(JMenu file, PianoTrack t) {
		JMenu modes = new JMenu("Arp");
		for (Arp m : Arp.values()) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(m.name());
			if (t.getArp() == m)
				item.setSelected(true);
			modes.add(item);
			mode.add(item);
			item.addActionListener(e-> t.setArp(m));
		}
		file.add(modes);
	}

	public void updateMode() {
		int i = 0;
		Enumeration<AbstractButton> it = mode.getElements();
		PianoTrack t = (PianoTrack)track;
		while (it.hasMoreElements())
			if (t.getArp().ordinal() == i++)
				it.nextElement().setSelected(true);
			else
				it.nextElement();
	}

	@Override public void update() {
		updateMode();
		updateCue();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		JudahZone.getSeq().getSynthTracks().setCurrent(track);
		//update();
	}

	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mouseReleased(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }

	@Override
	public void resized(int w, int h) {
		Gui.resize(this, new Dimension(w, h));
	}

}
