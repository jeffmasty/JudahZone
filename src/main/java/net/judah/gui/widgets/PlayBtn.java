package net.judah.gui.widgets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import judahzone.gui.Actionable;
import judahzone.gui.Pastels;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;
import net.judah.seq.Seq;
import net.judah.seq.arp.Arp;
import net.judah.seq.track.ChannelTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.TrackMenu.SendTo;
import net.judah.song.SongTrack;

public class PlayBtn extends JButton {
	private static final String COOL = " ▶️ ";

	private final MidiTrack track;
	private SongTrack expander;

	public PlayBtn(SongTrack s) {
		this(s.getTrack(), s.getTrack().getName());
		expander = s;
	}

	public PlayBtn(MidiTrack t) {
		this(t, "");
		update();
	}

	public PlayBtn(MidiTrack t, String lbl) {
		super(COOL + lbl);
		if (lbl.length() > 5)
			setToolTipText(lbl);
		track = t;
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
        Seq seq = JudahZone.getInstance().getSeq();
        popupMenu.add(new SendTo(track));
        if (track instanceof ChannelTrack)
        	popupMenu.add(new Actionable("Rename", e->seq.rename(track)));

        if (track instanceof PianoTrack piano) {
            popupMenu.add(new Actionable("Rename", e->seq.rename(piano)));
            if (track.isPermanent())
            	popupMenu.add(new Actionable("Clear Track", l->seq.clear(track)));
            else
            	popupMenu.add(new Actionable("Delete Track", l -> seq.confirmDelete(piano)));
        	popupMenu.add(new Actionable("MPK", e-> piano.toggle(Arp.MPK)));
        }
        else
        	popupMenu.add(new Actionable("Clear Track", l -> seq.clear(track)));
        popupMenu.add(new Actionable("Capture", e->track.setCapture(!track.isCapture())));
    	popupMenu.add(expander != null ? new Actionable("Automation", e->expander.expand()) :
    		new Actionable("Automation", e->MainFrame.setFocus(KnobMode.Autom8)));

    	popupMenu.show(PlayBtn.this, me.getX(), me.getY());
	}

	public void update() {
		setBackground(track.isCapture() ? Pastels.RED :
				track.isSynth() && ((PianoTrack)track).isMpkOn() ? Arp.MPK.getColor() :
				track.isActive() ? Pastels.GREEN :
				track.isOnDeck() ? track.getCue().getColor() : null);
	}
}
