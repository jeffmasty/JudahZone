package net.judah.seq.beatbox;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import net.judah.drumkit.DrumType;


public class Mutes extends JPanel {
	static final int PADS = DrumType.values().length;

	static final Border BORDER = new LineBorder(Pastels.GRID);
	private class Lbl extends JLabel {
		Lbl(String txt) {
			super(txt.toUpperCase(), JLabel.CENTER);
			setFont(Gui.FONT9);
			setBorder(BORDER);
		}
	}

	public Mutes() {
		setLayout(new GridLayout(PADS, 1, 0, 1));
		for (DrumType t : DrumType.values())
			add(new Lbl(t.name()));
	}

}
