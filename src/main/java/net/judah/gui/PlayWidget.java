package net.judah.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.judah.gui.JudahMenu.Actionable;
import net.judah.seq.arp.Arp;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.TrackMenu.SendTo;

public class PlayWidget extends JButton {

	private static final ArrayDeque<PlayWidget> instances = new ArrayDeque<>();
	private final MidiTrack track;

	public PlayWidget(MidiTrack t) {
		this(t, "");
	}

	public PlayWidget(MidiTrack t, String lbl) {
		super(lbl.isBlank() ? " ▶️  "  : "▶️ " + lbl);
		track = t;
		instances.add(this);
		setOpaque(true);
		addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
            	if (SwingUtilities.isRightMouseButton(e))
            		popup(e);
            	else
            		track.trigger();
            }

			private void popup(MouseEvent me) {
		        JPopupMenu popupMenu = new JPopupMenu();
		        SendTo sendTo = new SendTo(track);
		        popupMenu.add(sendTo);
		        popupMenu.add(new Actionable("Capture", e->track.setCapture(!track.isCapture())));
		        if (track.isSynth())
		        	popupMenu.add(new Actionable("MPK", e->((PianoTrack)track).toggle(Arp.MPK)));
		        popupMenu.show(PlayWidget.this, me.getX(), me.getY());

			}
        });
	}

	public static void update(MidiTrack t) {
		for (PlayWidget instance : instances)
			if (t == instance.track)
				instance.update();
	}

	void update() {
		setBackground(track.isCapture() ? Pastels.RED :
				track.isSynth() && ((PianoTrack)track).isMpkOn() ? Arp.MPK.getColor() :
				track.isActive() ? Pastels.GREEN :
				track.isOnDeck() ? track.getCue().getColor() : null);
	}
}
