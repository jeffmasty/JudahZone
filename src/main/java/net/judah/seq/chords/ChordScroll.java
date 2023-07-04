package net.judah.seq.chords;

import java.awt.GridLayout;
import java.util.ArrayDeque;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.gui.Gui;
import net.judah.gui.Pastels;

public class ChordScroll extends JPanel {

	private static ChordTrack chords;
	private static final Preview preview = new Preview();
	private static ArrayDeque<ChordScroll> instances = new ArrayDeque<>();
	private final JLabel a = new JLabel("   ", JLabel.LEFT);
	private final JLabel b = new JLabel("   ", JLabel.LEFT);
	private final JLabel c = new JLabel("   ", JLabel.LEFT);
	private final JLabel d = new JLabel("   ", JLabel.LEFT);
	private final JLabel e = new JLabel();
	private final JLabel aLyrics = new JLabel("    ", JLabel.LEFT);
	private final JLabel bLyrics = new JLabel("    ", JLabel.LEFT);
	private final JLabel[] crds;
	
	
	public ChordScroll(ChordTrack t) {
		chords = t;
		crds = new JLabel[] {a, b, c, d, e};
		JPanel top = new JPanel(new GridLayout(1, 5, 0, 0));
		for (JLabel it : crds) it.setFont(Gui.BOLD);
		for (JLabel it : crds) top.add(it);
		a.setOpaque(true);
		b.setOpaque(true);
		c.setOpaque(true);
		a.setBackground(Pastels.EGGSHELL);
		JPanel bottom = new JPanel();
		bottom.add(new JLabel("    "));
		bottom.add(aLyrics); bottom.add(bLyrics);
		
		aLyrics.setFont(Gui.BOLD);
		setLayout(new GridLayout(2, 1, 0, 0));
		add(top);
		add(bottom);
		setBorder(BorderFactory.createLineBorder(Pastels.BUTTONS, 2));
		
		instances.add(this);
		doLayout();
	}

	public static void scroll() {
		chords.preview(preview);
		for (ChordScroll it : instances) 
			it.publish();
	}
	
	private void publish() {
		if (preview.size() != crds.length) {
			for (JLabel crd : crds)
				crd.setText("");
			b.setBackground(null);
			c.setBackground(null);
			aLyrics.setText("");
			bLyrics.setText("");
			return;
		}
		Chord one = preview.get(1);
		Chord two = preview.get(2);
		Chord three = preview.get(3);
		Chord four = preview.get(4);
		
		a.setText(preview.get(0) == null ? "" : preview.get(0).getChord());
		b.setText(one == null ? " " : one.getChord());
		c.setText(two == null || two.equals(one )? "" : two.getChord());
		d.setText(three == null ? " " : three.getChord());
		e.setText(four == null || four.equals(three) ? "" : four.getChord());
		b.setBackground(preview.middle ? null : Pastels.BLUE);
		c.setBackground(preview.middle ? Pastels.BLUE : null);

		if (two != null && !two.equals(one) ) {
			StringBuffer sb = new StringBuffer(one == null ? "    " : one.getLyrics());
			aLyrics.setText(sb.append("-").append(two.getLyrics()).toString());
		}
		else 
			aLyrics.setText(one == null ? "" : one.getLyrics()); 
		
		if (four != null && !four.equals(three)) {
			StringBuffer sb = new StringBuffer(three == null ? "    " : three.getLyrics());
			aLyrics.setText(sb.append("-").append(four.getLyrics()).toString());
		}
		else
			bLyrics.setText(three == null ? "" : three.getLyrics()); 
	}

}

