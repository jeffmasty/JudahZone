package net.judah.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import net.judah.gui.widgets.TrackButton;
import net.judah.midi.JudahClock;
import net.judah.seq.TrackList;
import net.judah.seq.track.MidiTrack;

public class MiniSeq extends JPanel {
	private final Dimension TRX = new Dimension(Size.WIDTH_KNOBS / 2 - 15, 75);
	private final Border highlight = BorderFactory.createRaisedSoftBevelBorder();

	private final TrackList tracks;
	private final ArrayList<TrackButton> btns = new ArrayList<>();
	
	public MiniSeq(TrackList tracks, JudahClock clock) {
		this.tracks = tracks;
		
        setBorder(new LineBorder(Pastels.MY_GRAY, 1));
        Dimension plusOne = new Dimension(TRX.width + 1, TRX.height + 1);
        setPreferredSize(plusOne);
        setMaximumSize(plusOne);
        setLayout(new GridLayout(2, 5, 1, 1));
        setOpaque(true);
        
        tracks.forEach(t->btns.add(new TrackButton(t)));

        // [d1 d2 d3 d4 s5]
        // [s1 s2 s3 s4 s6]
        for (TrackButton b : btns) 
        	add(b);
        
        btns.get(8).setFont(btns.get(8).getFont().deriveFont(Font.ITALIC));
        btns.get(9).setFont(btns.get(9).getFont().deriveFont(Font.ITALIC));
        
        update();

	}

	public void update(MidiTrack t) {
		for (TrackButton b : btns)
			if (b.getTrack() == t) 
				b.update();
	}
	
	public void update() {
		MidiTrack t = tracks.getCurrent();
		btns.forEach(b->b.update());
		btns.forEach(b -> b.setBorder(t == b.getTrack() ? highlight : null));
		repaint();
	}
	
}
