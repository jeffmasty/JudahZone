package net.judah.gui.widgets;


import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.seq.arp.Arp;
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
				if (SwingUtilities.isRightMouseButton(e) && track instanceof PianoTrack synth)
					synth.toggle(Arp.MPK);
				else if (SwingUtilities.isMiddleMouseButton(e))
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
		setBorder(track.isCapture() ? Gui.RED : null);
		setBackground(bgColor(track));
	}

	public static Color bgColor(MidiTrack track) {
		return track.isActive() ?
				track.isSynth() && ((PianoTrack)track).isArpOn() ?
				((PianoTrack)track).getArp().getColor() : Pastels.GREEN
				: track.isOnDeck() ? track.getCue().getColor() : null;
	}


}
