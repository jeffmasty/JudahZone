package net.judah.tracker.edit;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.border.LineBorder;

import lombok.RequiredArgsConstructor;
import net.judah.tracker.Track;
import net.judah.util.Pastels;

@RequiredArgsConstructor
public class BeatLabel extends JLabel {

	private final Track track;
	
	Color bg = Color.WHITE;

	public BeatLabel(Track t, String s, Color background) {
        super(s);
        this.track = t;
		this.bg = background;
        setAlignmentX(0.42f);
        setBorder(new LineBorder(Pastels.GREEN, 1, true));
        setBackground(bg);
        setOpaque(true);
        setAlignmentX(0.5f);
	}
	
	
    public BeatLabel(Track t, String s) {
    	this(t, s, Color.WHITE);
    }

    public void setActive(boolean active) {
    	setBackground(active ? track.isActive() ? Color.GREEN : Pastels.MY_GRAY: bg);
        repaint();
    }

}
