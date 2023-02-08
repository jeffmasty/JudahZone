package net.judah.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import net.judah.JudahZone;
import net.judah.gui.settable.SetCombo;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.TrackButton;
import net.judah.midi.JudahClock;
import net.judah.seq.MidiTrack;
import net.judah.seq.TrackList;
import net.judah.song.Scene;
import net.judah.song.SongTab;

public class MiniSeq extends JPanel {
	private final Dimension TRX = new Dimension(Size.WIDTH_KNOBS / 2 - 15, 85);
	private final Border highlight = BorderFactory.createRaisedSoftBevelBorder();

	private final TrackList tracks;
	private final JudahClock clock;
	private final SongTab songs;
	private final Btn track = new Btn("", e->JudahZone.getSeq().getTracks().next(true));
	private final Btn scene = new Btn("OpenMic", e->JudahZone.getSongs().trigger());
	private final ArrayList<TrackButton> btns = new ArrayList<>();
	
	public MiniSeq(TrackList tracks, JudahClock clock, SongTab songs) {
		this.tracks = tracks;
		this.clock = clock;
		this.songs = songs;
		
		JPanel actives = new JPanel(); 
        actives.setBorder(new LineBorder(Pastels.MY_GRAY, 1));
        Dimension plusOne = new Dimension(TRX.width + 1, TRX.height + 1);
        actives.setPreferredSize(plusOne);
        actives.setMaximumSize(plusOne);
        actives.setLayout(new GridLayout(2, 5, 1, 1));
        actives.setOpaque(true);
        
        tracks.forEach(t->btns.add(new TrackButton(t)));

        // [d1 d2 d3 d4 s5]
        // [s1 s2 s3 s4 s6]
        for (int i = 0; i < 4; i++) 
        	actives.add(btns.get(i));
        actives.add(btns.get(8));
        for (int i = 4; i < 8; i++)
        	actives.add(btns.get(i));
        actives.add(btns.get(9));
        
        btns.get(8).setFont(btns.get(8).getFont().deriveFont(Font.ITALIC));
        btns.get(9).setFont(btns.get(9).getFont().deriveFont(Font.ITALIC));
        
        JPanel seqTitle = new JPanel();
        seqTitle.add(scene);
        seqTitle.add(track);

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(seqTitle);
        add(actives);
        update();

	}

	public void update(MidiTrack t) {
		for (TrackButton b : btns)
			if (b.getTrack() == t) 
				b.update();
	}
	
	public void sceneText() {
		if (songs.getCurrent() == null) 
			return;
		StringBuffer sb = new StringBuffer("Scene:");
		sb.append(1 + songs.getSong().getScenes().indexOf(songs.getCurrent()));
		Scene onDeck = songs.getOnDeck();
		if (onDeck != null) 
				sb.append("|").append(1 + songs.getSong().getScenes().indexOf(onDeck));
		if (SetCombo.getSet() != null)
			sb.append("!");
		scene.setText(sb.toString());
		scene.setBackground(onDeck == null ? null : onDeck.getType().getColor());
	}
	
	public void update() {
		MidiTrack t = tracks.getCurrent();
		track.setText(t.getName());
		track.setBackground(clock.isLooperSync() ? Pastels.YELLOW : null);
		btns.forEach(b->b.update());
		btns.forEach(b -> b.setBorder(t == b.getTrack() ? highlight : null));
		sceneText();
		repaint();
	}
	
}
