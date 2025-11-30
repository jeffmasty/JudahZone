package net.judah.gui.widgets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.judah.JudahZone;
import net.judah.gui.Actionable;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.knobs.KnobMode;
import net.judah.seq.arp.Arp;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.TrackMenu.SendTo;

public class PlayWidget extends JButton {
	private static final String COOL = " ▶️ ";

	private static final ArrayDeque<PlayWidget> instances = new ArrayDeque<>();
	private final MidiTrack track;

	public PlayWidget(MidiTrack t) {
		this(t, "");
	}

	public PlayWidget(MidiTrack t, String lbl) {
		super(COOL + lbl);
		if (lbl.length() > 5)
			setToolTipText(lbl);
		track = t;
		instances.add(this);
		setOpaque(true);
		addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
            	if (SwingUtilities.isRightMouseButton(e))
            		popup(e);
            	else
            		track.trigger();
            }});
	}

	private void popup(MouseEvent me) {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(new SendTo(track));
        if (track instanceof PianoTrack piano) {
            popupMenu.add(new Actionable("Rename", e->JudahZone.getSeq().rename(piano)));
            if (track.isPermanent())
            	popupMenu.add(new Actionable("Clear Track", l->JudahZone.getSeq().clear(track)));
            else
            	popupMenu.add(new Actionable("Delete Track", l -> JudahZone.getSeq().confirmDelete(piano)));
        	popupMenu.add(new Actionable("MPK", e-> piano.toggle(Arp.MPK)));
        }
        else
        	popupMenu.add(new Actionable("Clear Track", l -> JudahZone.getSeq().clear(track)));

        popupMenu.add(new Actionable("Capture", e->track.setCapture(!track.isCapture())));
    	popupMenu.add(new Actionable("Automation", e->MainFrame.setFocus(KnobMode.Autom8))); // TODO
        popupMenu.show(PlayWidget.this, me.getX(), me.getY());
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
