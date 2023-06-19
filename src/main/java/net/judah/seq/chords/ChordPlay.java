package net.judah.seq.chords;

import static net.judah.gui.Pastels.*;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import net.judah.util.Constants;

public class ChordPlay extends JButton {
	
	private static final HashSet<ChordPlay> instances = new HashSet<>();
	private static ChordTrack chords;
	
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
	
	
	public static void update() {
		Constants.execute(()->{
			for (ChordPlay btn : instances) {
				btn.setBackground(chords.isActive() ? GREEN : chords.isOnDeck() ? YELLOW : null);
				btn.setEnabled(chords.getSection() != null);
			}
		});
	}
}
