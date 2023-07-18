package net.judah.seq.chords;

import static net.judah.gui.Pastels.GREEN;
import static net.judah.gui.Pastels.YELLOW;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import net.judah.seq.MidiConstants;
import net.judah.util.Constants;

public class ChordPlay extends JButton implements ChordListener {
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
		Constants.execute(()->setText(to == null ? FANCY : to.getChord()));
	}

	public ChordPlay makeFancy() {
		if (!chords.getListeners().contains(this))
			chords.addListener(this);
		return this;
	}
	
	public static void update() {
		Constants.execute(()->{
			for (ChordPlay btn : instances) {
				btn.setBackground(chords.isActive() ? GREEN : chords.isOnDeck() ? YELLOW : null);
				btn.setEnabled(chords.getSection() != null);
			}
		});
	}
}
