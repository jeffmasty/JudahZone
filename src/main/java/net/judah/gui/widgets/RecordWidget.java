package net.judah.gui.widgets;

import javax.swing.JButton;

import net.judah.gui.Pastels;
import net.judah.gui.Updateable;
import net.judah.seq.track.MidiTrack;

public class RecordWidget extends JButton implements Updateable {

	private final MidiTrack track;

	public RecordWidget(String lbl, MidiTrack t) {
		super(lbl);
		this.track = t;
		addActionListener(e->track.setCapture(!track.isCapture()));
		setOpaque(true);
		update();
	}

	public RecordWidget(MidiTrack t) {
		this(" ⏺ ", t);
	}

	@Override
	public void update() {
		setBackground(track.isCapture() ? Pastels.RED : null);
	}

}
