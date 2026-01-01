package net.judah.seq.piano;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import judahzone.gui.Actionable;
import judahzone.gui.Floating;
import judahzone.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.settable.ModeCombo;
import net.judah.gui.widgets.Arrow;
import net.judah.gui.widgets.GateCombo;
import net.judah.gui.widgets.Integers;
import net.judah.seq.Duration;
import net.judah.seq.Seq;
import net.judah.seq.Transpose;
import net.judah.seq.track.Computer.Update;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.TrackMenu;

public class PianoMenu extends TrackMenu implements Floating {

	// private final ButtonGroup mode = new ButtonGroup();
	private final GateCombo gate;
	private final ModeCombo mode;
	private final JComboBox<Integer> range = new JComboBox<Integer>(Integers.generate(0, 88));

	public PianoMenu(PianoView view, Piano grid, Seq seq) {
		super(grid, seq.getAutomation());
		PianoTrack t = (PianoTrack) track;
		gate = new GateCombo(t);
		mode = new ModeCombo(t);
		// additional non-drum stuff
		add(new JLabel(" Arp"));
		add(mode);
		add(new JLabel("Span"));

		range.setSelectedItem(t.getRange());
		add(Gui.resize(range, Size.MICRO));

		zoomMenu(view);
		// arpMenu(file, t);
		tools.add(new Actionable("Remap...", e->new Transpose(grid)));
		file.add(new Actionable("Rename", e->seq.rename(t)));
		file.add(new Actionable("Delete", e->seq.confirmDelete(t)));
		edit.add(new Actionable("Duration...", e->new Duration(grid)));
		add(Box.createHorizontalStrut(4));
		add(new JLabel("Amp "));
		add(velocity);
		add(new JLabel(" Gate"));
		add(Gui.resize(gate, Size.SMALLER));
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

//	private void arpMenu(JMenu file, PianoTrack t) {
//		JMenu modes = new JMenu("Arp");
//		for (Arp m : Arp.values()) {
//			JRadioButtonMenuItem item = new JRadioButtonMenuItem(m.name());
//			if (t.getArp() == m)
//				item.setSelected(true);
//			modes.add(item);
//			mode.add(item);
//			item.addActionListener(e-> t.setArp(m));
//		}
//		file.add(modes);
//	}

	@Override
	public void update(Update type) {
		if (type == Update.ARP)
			mode.update();
		else if (type == Update.GATE)
			gate.update();
		else if (type == Update.RANGE)
			range.setSelectedItem(((PianoTrack)track).getRange());
		else
			super.update(type);
	}

	@Override
	public void mousePressed(MouseEvent e) {
//		JudahZone.getSeq().getSynthTracks().setCurrent(track);
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
