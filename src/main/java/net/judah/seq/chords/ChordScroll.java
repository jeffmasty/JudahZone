package net.judah.seq.chords;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayDeque;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import judahzone.api.Chord;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;

public class ChordScroll extends JPanel {

	private static Chords chords;
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
	
	
	public ChordScroll(Chords t) {
		chords = t;
		crds = new JLabel[] {a, b, c, d, e};
		aLyrics.setFont(Gui.BOLD11);
        
        JPanel top = new JPanel(new GridLayout(1, 5, 1, 1));
		for (JLabel it : crds) it.setFont(Gui.BOLD11);
		for (JLabel it : crds) top.add(it);
		a.setOpaque(true);
		b.setOpaque(true);
		c.setOpaque(true);
		a.setBackground(Pastels.BG);
		
		JPanel bottom = new JPanel();
		GridBagConstraints grid = new GridBagConstraints();
		grid.ipadx = 0;
        grid.fill = GridBagConstraints.HORIZONTAL;
        grid.insets = new Insets(0, 0, 0, 0);
        grid.anchor = GridBagConstraints.LINE_START;
		GridBagLayout layout = new GridBagLayout(); 
        bottom.setLayout(layout);

		
        JLabel blank = new JLabel("    ");
        layout.setConstraints(blank, grid);
        grid.gridwidth = 2;
        grid.gridx = 1;
        layout.setConstraints(aLyrics, grid);
        grid.gridx = 3;
        layout.setConstraints(bLyrics, grid);
		
        bottom.add(blank);
        bottom.add(aLyrics);
        bottom.add(bLyrics);
        
		setLayout(new GridLayout(2, 1, 1, 2));
		add(top);
		add(bottom);
		setBorder(BorderFactory.createLineBorder(Pastels.BUTTONS, 2));
		
		instances.add(this);
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
		c.setText(two == null || two.equals(one) ? "" : two.getChord());
		d.setText(three == null ? " " : three.getChord());
		e.setText(four == null || four.equals(three) ? "" : four.getChord());
		
		if (two == null || two.equals(one))
			aLyrics.setText(one == null ? "   " : one.getLyrics()); 
		else {
			StringBuffer sb = new StringBuffer(one == null ? "    " : one.getLyrics());
			sb.append("-").append(two.getLyrics());
			aLyrics.setText(sb.toString());
		}

		if (four == null || four.equals(three))
			bLyrics.setText(three == null ? "    " : three.getLyrics());
		else {
			StringBuffer sb = new StringBuffer(three == null ? "    " : three.getLyrics());
			sb.append("-").append(four.getLyrics());
			bLyrics.setText(sb.toString());
		}

		b.setBackground(preview.middle ? null : Pastels.BLUE);
		c.setBackground(preview.middle ? Pastels.BLUE : null);

	}

}

