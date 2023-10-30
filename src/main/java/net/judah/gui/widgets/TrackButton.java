package net.judah.gui.widgets;


import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Pastels;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;

public class TrackButton extends JLabel {
	
	@Getter private final MidiTrack track;
	
	public TrackButton(MidiTrack t) {
		setHorizontalAlignment(SwingConstants.CENTER);
		this.track = t;
		setBorder(null);
		setOpaque(true);
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getButton() == 3) 
					JudahZone.getSeq().getTracks().setCurrent(track);
				else 
					track.trigger();
			}});
		update();
	}
	
	public void update() {
		int idx = 1 + track.getCurrent() / 2;
		if (idx < 10)
			setText(" " + idx);
		else setText("" + idx);
		
		setBackground(bgColor(track));
	}

	public static Color bgColor(MidiTrack track) {
		return track.isActive() ? 
				track.isSynth() && ((PianoTrack)track).getArp().isActive() ? 
				((PianoTrack)track).getArp().getMode().getColor() : Pastels.GREEN
				: track.isOnDeck() ? track.getCue().getColor() : null;
	}

	
}
