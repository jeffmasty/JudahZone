package net.judah.gui;

import java.util.ArrayDeque;

import javax.swing.JButton;

import net.judah.seq.track.MidiTrack;
import net.judah.util.Constants;

public class RecordWidget extends JButton implements Updateable {

	private static final ArrayDeque<RecordWidget> instances = new ArrayDeque<>();
	private final MidiTrack track;
	
	public RecordWidget(MidiTrack t) {
		super("  ⏺  ");
		this.track = t;
		addActionListener(e->track.setRecord(!track.isRecord()));
		setOpaque(true);
		instances.add(this);
	}
	
	@Override
	public void update() {
		setBackground(track.isRecord() ? Pastels.RED : null);
		MainFrame.miniSeq().update(track);
	}
	
	public static void update(MidiTrack track) {
		Constants.execute(()->{
			for (RecordWidget widget: instances)
				if (widget.track == track)
					widget.update();
		});
	}

}
