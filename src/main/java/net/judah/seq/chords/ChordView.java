package net.judah.seq.chords;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import lombok.Setter;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.widgets.Btn;
import net.judah.song.Song;

/** Small window inside Overview */
public class ChordView extends JPanel {
	public static final Color BG = Pastels.BUTTONS;

	private final ChordTrack chords;
	private final JLabel bar = new JLabel(" 0 ", JLabel.CENTER);
	private final JLabel bars = new JLabel(" 0 ", JLabel.CENTER);
	private final Btn next;
	private final JToggleButton loop = new JToggleButton("🔁");
	@Setter private Song song;
	private Section section;

	public ChordView(ChordTrack trk) {
		this.chords = trk;
		next = new Btn(" ▶| ", e->chords.next());
		loop.setSelected(trk.getSection() != null && trk.getSection().isOnLoop());
		loop.addActionListener(e->loop());

		JPanel play = new JPanel(new GridLayout(2, 1, 0, 0));
		play.add(new ChordPlay("▶️ Chords", chords));
		JPanel btns = Gui.wrap(loop, next);
		play.add(btns);
		JPanel top = Gui.wrap(new JLabel("File "), new ChordProCombo());
		JPanel bottom = Gui.wrap(new JLabel("Part"), new SectionCombo(chords));
		JPanel box = new JPanel(new GridLayout(2, 1, 0, 0));
		box.add(top);
		box.add(bottom);
		JPanel tail = new JPanel();
		tail.setLayout(new BoxLayout(tail, BoxLayout.PAGE_AXIS));
		Dimension tiny = new Dimension(24, Size.STD_HEIGHT - 1);
		tail.add(Gui.resize(bar, tiny));
		tail.add(new JSeparator(SwingConstants.HORIZONTAL));
		tail.add(Gui.resize(bars, tiny));
		for (JComponent comp : new JComponent[] {this, bar, bars, next, top, bottom, btns, tail})
			comp.setBackground(BG);

		setOpaque(true);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(play);
		add(box);
		add(Gui.resize(new ChordScroll(chords), new Dimension(350, 92)));
		add(tail);
		add(new JLabel("bars"));
	}

	public void setSection(Section s) {
		this.section = s;
		if (section == null) {
			bars.setText("--");
			next.setEnabled(false);
		}
		else {
			bars.setText("" + chords.bars(section.getCount()));
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
