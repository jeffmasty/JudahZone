package net.judah.gui;

import java.util.ArrayDeque;

import javax.swing.JButton;

import net.judah.seq.track.MidiTrack;

public class PlayWidget extends JButton {

	private static final ArrayDeque<PlayWidget> instances = new ArrayDeque<>();
	private final MidiTrack track;
	
	public PlayWidget(MidiTrack t) {
		this(t, "");
	}
	
	public PlayWidget(MidiTrack t, String lbl) {
		super(lbl.isBlank() ? "  ▶️  "  : "▶️ " + lbl);
		this.track = t;
		instances.add(this);
		setOpaque(true);
		addActionListener(e->track.trigger());
	}

	public static void update(MidiTrack t) {
		for (PlayWidget instance : instances)
			if (instance.track == t) {
				instance.setBackground(t.isActive() ? Pastels.GREEN : t.isOnDeck() ? t.getCue().getColor() : null);
			}
	}
	
}
