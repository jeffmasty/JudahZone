package net.judah.seq.chords;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import net.judah.gui.Pastels;
import net.judah.omni.Threads;
import net.judah.seq.MidiConstants;

public class ChordPlay extends JButton implements ChordListener, Pastels {
	public static final String FANCY = " " + MidiConstants.SHARP + " " + MidiConstants.FLAT + " ";

	private static final HashSet<ChordPlay> instances = new HashSet<>();
	private static ChordTrack chords;

	public ChordPlay(ChordTrack track) {
		this(FANCY, track);
	}

	public ChordPlay(String lbl, ChordTrack track) {
		super(lbl);
		ChordPlay.chords = track;
		setOpaque(true);
		addMouseListener(new MouseAdapter() {
        @Override public void mouseClicked(MouseEvent me) {
        	if (SwingUtilities.isRightMouseButton(me)) {
        		if (chords.getSection() == null || chords.getSections().isEmpty()) return;
        		if (chords.getSections().size() == 1)
        			chords.toggle(Directive.LOOP);
        		else
        			chords.getSection().toggle(Directive.LOOP);
        	}
        	else {
        		chords.toggle();
        	}
        }});
		instances.add(this);
	}

	@Override
	public void chordChange(Chord from, Chord to) {
		Threads.execute(()->setText(to == null ? FANCY : to.getChord()));
	}

	public ChordPlay makeFancy() {
		if (!chords.getListeners().contains(this))
			chords.addListener(this);
		return this;
	}

	public static void update() {
		Threads.execute(()->{
			for (ChordPlay btn : instances) {
				btn.setBackground(chords.isActive() ? GREEN : chords.isOnDeck() ? YELLOW : null);
				btn.setEnabled(chords.getSection() != null);
			}
		});
	}
}
