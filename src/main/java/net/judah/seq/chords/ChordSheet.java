package net.judah.seq.chords;

import static net.judah.gui.Pastels.BUTTONS;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.Click;
import net.judah.song.Song;
import net.judah.util.RTLogger;

/** Full window view */
public class ChordSheet extends JPanel {
	private static final int TABS = 4;
	private static final int WIDTH = Size.WIDTH_TAB - 32;
	private static final int LBLS = 70;
	private static final int PAD = 4;
	private static final Dimension COLUMN = new Dimension( (WIDTH - LBLS - PAD - 30) / 4, 43);

	private int measure;
	private final Chords chords;
	private JScrollPane scroll = new JScrollPane();
	private JPanel content;

	private final ArrayList<Crd> parts = new ArrayList<>();
	private final ArrayList<Group> groups = new ArrayList<>();
	private final JLabel directives = new JLabel();
	private final JToggleButton loop = new JToggleButton(" 🔁 ");

	public ChordSheet(Chords chrds) {
		this.chords = chrds;
		setName("Chords");
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setAutoscrolls(true);
		scroll.getVerticalScrollBar().setUnitIncrement(20);

		loop.setSelected(chords.getDirectives().contains(Directive.LOOP));
		loop.addActionListener(e->chords.toggle(Directive.LOOP));

		JComponent top = new Box(BoxLayout.LINE_AXIS);
		top.add(Box.createHorizontalStrut(8));
		top.add(new ChordPlay("▶️ Chords", chords));
		top.add(Gui.resize(new ChordProCombo(), Size.WIDE_SIZE));
		top.add(new Btn("Edit", e->edit()));
		top.add(Gui.resize(new SectionCombo(chords), Size.COMBO_SIZE));
		top.add(loop);
		top.add(directives);
		top.add(Box.createHorizontalGlue());

		add(Gui.wrap(top));
		add(Box.createVerticalStrut(5));
		add(scroll);
		add(Box.createVerticalGlue());
		refresh();
	}

	void edit() {
		Song song = JudahZone.getOverview().getSong();
		if (song == null || song.getChordpro() == null || song.getChordpro().isBlank())
			return;
		try {
			Desktop.getDesktop().open(Chords.fromSong(song));
		} catch (IOException e) { RTLogger.warn(this, e.getMessage()); }
	}

	public void update(Chord chord) {
		for (int i = 0; i < parts.size(); i++) {
			Crd crd = parts.get(i);
			if (crd.getBackground() == Pastels.BLUE)
				if (crd.chord != chord)
					crd.setBackground(Color.WHITE);
			if (crd.chord == chord) {
				crd.scrollRectToVisible(crd.getBounds());
				crd.setBackground(Pastels.BLUE);
			}
		}
		for (int i = 0; i < groups.size(); i++)
			if (groups.get(i).section == chords.getSection())
				groups.get(i).update();

		scroll.repaint();
	}

	public void refresh() {
		parts.clear();
		scroll.getViewport().removeAll();
		content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
		content.setOpaque(true);
		measure = chords.getSig().steps;
		groups.clear();
		for (int i = 0 ; i < chords.getSections().size(); i++)
			new Group(chords.getSections().get(i));
		scroll.setViewportView(content);
		updateDirectives();
	}

	public void setSection(Section part) {
		for (int i = 0; i < groups.size(); i++)
			groups.get(i).highlight(part);
	}


	private void length() {
		if (chords.getSection() == null) return;
		JudahZone.getClock().setLength(chords.bars(chords.getSection().getCount()));
	}
	class Crd extends JLabel {
		final Chord chord;
		Crd(Chord chord) {
			this.chord = chord;
			String text ="<html><b>" + chord.getChord(); // TODO TimeSig
			text += "</b><BR/>";
			text += chord.getLyrics() == null || chord.getLyrics().isBlank() ? "&nbsp;" : chord.getLyrics();
			text += "</html>";
			setText(text);
			setOpaque(true);
			setBackground(Color.white);
			parts.add(this);
			addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent me) {
					if (SwingUtilities.isRightMouseButton(me)) {
						chord.build();
						RTLogger.log(this, chord.toString());
					}
					else
						chords.click(chord);
				}});
		}
	}

	private class Group extends JPanel {

		final Section section;
		JToggleButton loop = new JToggleButton("🔁");
		JLabel bar = new JLabel("1", JLabel.CENTER);
		Click total = new Click(" 0 ", e->length());
		JPanel left = new JPanel();
		JPanel lbls = new JPanel();
		JPanel btns = new JPanel();

		Group(Section s) {
			groups.add(this);
			section = s;

			int measures = chords.bars(section.getCount());
			total.setText(" / " + measures);
			loop.setSelected(section.isOnLoop());
			loop.addActionListener(e->section.toggle(Directive.LOOP));
			loop.setAlignmentX(0);

			lbls.add(bar);
			lbls.add(total);

			left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));
			left.add(lbls);
			btns.add(loop);
			left.add(btns);

			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			add(left);
			add(Box.createHorizontalStrut(PAD));
			add(createContent());

			setBorder(BorderFactory.createTitledBorder(s.getName()));
			setOpaque(true);
			content.add(this);
			addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					chords.setSection(section, true);
				};
			});
		}

		JPanel createContent() {
			JPanel result = new JPanel(new GridLayout(0, TABS, 0, 0));
			result.setBackground(Color.WHITE);
			JLabel onDeck = null;
			for (Chord chord : section) {
				if (onDeck != null) {
					JPanel duo = new JPanel(new GridLayout(1, 2));
					duo.add(onDeck);
					duo.add(new Crd(chord));
					result.add(duo);
					onDeck = null;
				}
				else if (chord.steps < measure)
					onDeck = new Crd(chord);
				else result.add(new Crd(chord));
			}
			if (onDeck != null)
				result.add(onDeck);
			for (int i = 0; i < result.getComponentCount(); i++)
				Gui.resize(result.getComponent(i), COLUMN);
			return result;
		}

		void update() {
			bar.setText("" + (chords.getBar() + 1));
		}

		void highlight(Section s) {
			Color bg = s == section ? BUTTONS : null;
			setBackground(bg);
			left.setBackground(bg);
			lbls.setBackground(bg);
			btns.setBackground(bg);
		}
	}

	public void updateDirectives() {
		loop.setSelected(chords.getDirectives().contains(Directive.LOOP));
		String dir = " ";
		for (Directive d : chords.getDirectives())
			if (d != Directive.LOOP)
				dir += d.name() + " ";
		directives.setText(dir);
	}

}
