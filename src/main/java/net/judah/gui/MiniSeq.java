package net.judah.gui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.Border;

import net.judah.gui.widgets.TrackButton;
import net.judah.midi.JudahClock;
import net.judah.seq.TrackList;
import net.judah.seq.track.MidiTrack;

public class MiniSeq extends Box {
	private final Dimension TRX = new Dimension(Size.WIDTH_KNOBS / 2 - 10, 73);
	private final Border highlight = BorderFactory.createRaisedSoftBevelBorder();

	private final TrackList<MidiTrack> tracks;
	private final ArrayList<TrackButton> btns = new ArrayList<>();

	public MiniSeq(TrackList<MidiTrack> tracks, JudahClock clock) {
		super(BoxLayout.X_AXIS);
		this.tracks = tracks;
        //setBorder(new LineBorder(Pastels.MY_GRAY, 1));
        Gui.resize(this, TRX);
        setOpaque(true);

        tracks.forEach(t->btns.add(new TrackButton(t)));

        // D1 D2   B S1 S2
        // D3 D4  F1 F2 F3
        JPanel drums = new JPanel(new GridLayout(2, 2, 1, 1));
        drums.setBorder(BorderFactory.createLineBorder(Pastels.MY_GRAY));
        JPanel synths = new JPanel(new GridLayout(2, 3, 1, 1));

        for (TrackButton btn : btns)
        	if (btn.getTrack().isDrums())
        		drums.add(btn);
        	else
        		synths.add(btn);
        add(drums);
        add(synths);
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
