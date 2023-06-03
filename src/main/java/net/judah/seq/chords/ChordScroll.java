package net.judah.seq.chords;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.gui.Updateable;

public class ChordScroll extends JPanel implements Updateable {

	private final ChordTrack chords;
	
	private final Crd present;
	
	public ChordScroll(ChordTrack trk) {
		chords = trk;
		setLayout(new GridLayout(1, 5));
		present = new Crd();
		add(present);
	}
	/*
		StringBuffer c = new StringBuffer();// .append(chords.bars()).append(" bars  ");
		if (!chords.getSections().isEmpty())
			for (Chord p : chords.getSections().get(0).getChords()) 
				c.append(p.getChord().toString());
		text.setText(c.toString());
	 */
	@Override
	public void update() {
		// past.update();
		if (chords.getChord() != null)
			present.setText(chords.getChord().getChord());
		//repaint();
	}

	class Crd extends JLabel {
		
		void update() {
			Chord update = chords.getChord();
			if (update == null) 
				setText(chords.getSection().getName());
			else if (update.getChord().equals(getText()))
				return;
			else 
				setText(update.getChord());
		}
	}
	
}
