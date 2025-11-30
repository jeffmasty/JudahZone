package net.judah.gui.widgets;


import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.seq.Seq;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;

public class TrackButton extends JLabel {
	private static final Border highlight = BorderFactory.createRaisedSoftBevelBorder();

	@Getter private final MidiTrack track;
	private final Seq seq;

	public TrackButton(MidiTrack t, Seq seq) {
		this.seq = seq;
		this.track = t;
		setHorizontalAlignment(SwingConstants.CENTER);
		setBorder(null);
		setOpaque(true);
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e))
					track.setCapture(!track.isCapture());
				else if (SwingUtilities.isMiddleMouseButton(e))
					JudahZone.getSeq().getTracks().setCurrent(track);
				else
					track.trigger();
			}});
		setToolTipText(t.getName());
		update();
	}

	public void update() {
		int idx = 1 + track.getCurrent() / 2;
		if (idx < 10)
			setText(" " + idx);
		else setText("" + idx);
		setBorder(track.isCapture() ? Gui.RED : track == seq.getCurrent() ? highlight : null);
		setBackground(bgColor(track));
	}

	public static Color bgColor(MidiTrack track) {
		return track.isActive() ?
				track.isSynth() && ((PianoTrack)track).isArpOn() ?
				((PianoTrack)track).getArp().getColor() : Pastels.GREEN
				: track.isOnDeck() ? track.getCue().getColor() : null;
	}

}
