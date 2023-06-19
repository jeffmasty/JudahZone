package net.judah.seq.chords;

import java.awt.GridLayout;
import java.util.ArrayDeque;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import net.judah.gui.Pastels;

public class ChordScroll extends JPanel {

	private static ChordTrack chords;
	private static final Preview preview = new Preview();
	private static ArrayDeque<ChordScroll> instances = new ArrayDeque<>();
	private final JLabel a = new JLabel();
	private final JLabel b = new JLabel();
	private final JLabel c = new JLabel();
	private final JLabel d = new JLabel();
	private final JLabel e = new JLabel();
	private final JLabel[] crds;
	
	
	public ChordScroll(ChordTrack t) {
		chords = t;
		if (chords == null) throw new NullPointerException("ChordTrack not initialized.");
		setLayout(new GridLayout(1, 5, 0, 0));
		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		add(a); 		
		add(b); add(c); 
		add(d); add(e); 
		b.setOpaque(true);
		c.setOpaque(true);
		instances.add(this);
		crds = new JLabel[] {a, b, c, d, e};
	}

	public static void scroll() {
		chords.preview(preview);
		for (ChordScroll it : instances) 
			it.publish();
	}
	
	private void publish() {
		if (preview.size() == crds.length) {
			a.setText(preview.get(0) == null ? " " : preview.get(0).getChord());
			b.setText(preview.get(1) == null ? " " : preview.get(1).getChord());
			c.setText(preview.get(2) == null ? " " : 
				preview.get(2).equals(preview.get(1)) ? " " : preview.get(2).getChord());
			d.setText(preview.get(3) == null ? " " : preview.get(3).getChord());
			e.setText(preview.get(4) == null ? " " : 
				preview.get(4).equals(preview.get(3)) ? " " : preview.get(4).getChord());
			b.setBackground(preview.middle ? null : Pastels.BLUE);
			c.setBackground(preview.middle ? Pastels.BLUE : null);
		}
		else {
			for (JLabel crd : crds)
				crd.setText("");
			b.setBackground(null);
			c.setBackground(null);
		}
			
		
	}

}

