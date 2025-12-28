package net.judah.song;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import net.judah.api.Chord;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.widgets.Btn;
import net.judah.seq.chords.ChordPlay;
import net.judah.seq.chords.ChordProCombo;
import net.judah.seq.chords.ChordScroll;
import net.judah.seq.chords.Chords;
import net.judah.seq.chords.Directive;
import net.judah.seq.chords.Section;
import net.judah.seq.chords.SectionCombo;

/** Small window inside Overview */
public class ChordTrack extends Gui.Opaque {
	public static final Color BG = Pastels.BUTTONS;

	public static int HEIGHT = 50;

	private final Chords chords;
	private final JLabel bar = new JLabel(" 0 ", JLabel.CENTER);
	private final JLabel bars = new JLabel(" 0 ", JLabel.CENTER);
	private final Btn next;
	private final JToggleButton loop = new JToggleButton("🔁");
	private Section section;

	public ChordTrack(Chords trk) {
		this.chords = trk;
		next = new Btn(" ▶| ", e->chords.next());
		loop.setSelected(trk.getSection() != null && trk.getSection().isOnLoop());
		loop.addActionListener(e->loop());
		JPanel play = new JPanel(new GridLayout(2, 1, 0, 0));
		play.add(Gui.resize(new ChordPlay("▶️ Chords", chords), Size.SMALLER));
		JPanel btns = Gui.wrap(loop, next);
		play.add(btns);
		JPanel top = Gui.wrap(new JLabel("File "), new ChordProCombo());
		JPanel bottom = Gui.wrap(new JLabel("Part"), new SectionCombo(chords));
		JPanel box = new JPanel(new GridLayout(2, 1, 0, 0));
		box.add(top);
		box.add(bottom);
		Box tail = new Box(BoxLayout.Y_AXIS);
		Dimension tiny = new Dimension(38, Size.STD_HEIGHT - 1);
		tail.add(Gui.resize(bar, tiny));
		tail.add(new JSeparator(SwingConstants.HORIZONTAL));
		tail.add(Gui.resize(bars, tiny));
		for (JComponent comp : new JComponent[] {this, bar, bars, next, top, bottom, btns, play, tail})
			comp.setBackground(BG);

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(play);
		add(box);
		add(Gui.resize(new ChordScroll(chords), new Dimension(320, HEIGHT)));
		add(tail);
		add(Box.createHorizontalStrut(1));
		add(Box.createHorizontalGlue());
	}

	public void setSection(Section s) {
		this.section = s;
		if (section == null) {
			bars.setText("--");
			next.setEnabled(false);
		}
		else {
			bars.setText(chords.bars(section.getCount()) + "");
			next.setEnabled(true);
		}
		updateDirectives();
	}

	public void updateDirectives() {
		if (section == null)
			loop.setSelected(false);
		else if (loop.isSelected() != section.isOnLoop())
			loop.setSelected(section.isOnLoop());
	}

	public void update(Chord chord) {
		bar.setText("" + (chords.getBar() + 1));
	}

	private void loop() {
		if (chords.getSection() == null)
			chords.load();
		else
			chords.getSection().toggle(Directive.LOOP);
	}

}
