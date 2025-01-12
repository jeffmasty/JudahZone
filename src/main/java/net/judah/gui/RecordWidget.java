package net.judah.gui;

import java.util.ArrayDeque;

import javax.swing.JButton;

import net.judah.omni.Threads;
import net.judah.seq.track.MidiTrack;

public class RecordWidget extends JButton implements Updateable {

	private static final ArrayDeque<RecordWidget> instances = new ArrayDeque<>();
	private final MidiTrack track;

	public RecordWidget(String lbl, MidiTrack t) {
		super(lbl);
		this.track = t;
		addActionListener(e->track.setCapture(!track.isCapture()));
		setOpaque(true);
		instances.add(this);
	}

	public RecordWidget(MidiTrack t) {
		this(" ⏺ ", t);
	}

	@Override
	public void update() {
		setBackground(track.isCapture() ? Pastels.RED : null);
		MainFrame.miniSeq().update(track);
		PlayWidget.update(track);
	}

	public static void update(MidiTrack track) {
		Threads.execute(()->{
			//JudahZone.getMidiGui().update(track);
			for (RecordWidget widget: instances)
				if (widget.track == track)
					widget.update();
		});
	}

}
