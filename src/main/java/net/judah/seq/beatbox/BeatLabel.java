package net.judah.seq.beatbox;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.border.LineBorder;

import lombok.RequiredArgsConstructor;
import net.judah.gui.Pastels;
import net.judah.seq.track.MidiTrack;

@RequiredArgsConstructor
public class BeatLabel extends JLabel {

	private final MidiTrack track;
	
	Color bg = Color.WHITE;

	public BeatLabel(MidiTrack t, String s, Color background) {
        super(s);
        this.track = t;
		this.bg = background;
        setAlignmentX(0.42f);
        setBorder(new LineBorder(Pastels.GREEN, 1, true));
        setBackground(bg);
        setOpaque(true);
        setAlignmentX(0.5f);
	}
	
	
    public BeatLabel(MidiTrack t, String s) {
    	this(t, s, Color.WHITE);
    }

    public void setActive(boolean active) {
    	setBackground(active ? track.isActive() ? Color.GREEN : Pastels.MY_GRAY: bg);
        repaint();
    }

}
