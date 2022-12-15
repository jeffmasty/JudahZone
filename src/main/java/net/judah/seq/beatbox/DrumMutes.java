package net.judah.seq.beatbox;

import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.judah.drumkit.DrumType;
import net.judah.seq.MidiTrack;
import net.judah.util.Constants;

public class DrumMutes extends JPanel {
	
	private final MidiTrack track;
	private final Rectangle r;
	private final int rowHeight;
	
	private class Mute extends JButton {
		Mute(DrumType type, int y) {
			super(type.name());
			setFont(Constants.Gui.FONT11);
			addActionListener(e -> track.toggleMute(type));
			setBounds(0, y * rowHeight, r.width, rowHeight);
		}
	}
	
	public DrumMutes(Rectangle r, MidiTrack t) {
		this.r = r;
		this.track = t;
		setBounds(r);
		setLayout(null);
		rowHeight = (int)Math.ceil((r.height) / DrumType.values().length);
		for (int y = 0; y < DrumType.values().length; y++) {
			add(new Mute(DrumType.values()[y], y));
		}
		
	}
	
	
	

}
