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

import javax.swing.*;

import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.Click;
import net.judah.song.Song;
import net.judah.util.RTLogger;

public class ChordSheet extends JPanel {
	private static final int TABS = 4;
	private static final int WIDTH = Size.WIDTH_TAB - 50;
	
	private int measure;
	private final ChordTrack chords;
	private JScrollPane scroll = new JScrollPane();
	private JPanel content;
	
	private final ArrayList<Crd> parts = new ArrayList<>();
	private final ArrayList<Group> groups = new ArrayList<>();
	private final JLabel directives = new JLabel();
	private final JToggleButton loop = new JToggleButton(" 🔁 ");
	
	public ChordSheet(ChordTrack chrds) {
		this.chords = chrds;
		setName("Chords");
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setAutoscrolls(true);
		Gui.resize(scroll, new Dimension(WIDTH, Size.HEIGHT_TAB - 80));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
		top.add(Box.createHorizontalStrut(10));
		top.add(new ChordPlay("▶️ Chords", chords));
		top.add(Gui.resize(new ChordProCombo(), Size.WIDE_SIZE));
		top.add(new Btn("Edit", e->edit()));
		
		top.add(Gui.resize(new SectionCombo(chords), Size.COMBO_SIZE));
		top.add(loop);
		top.add(directives);
		loop.setSelected(chords.getDirectives().contains(Directive.LOOP));
		loop.addActionListener(e->chords.toggle(Directive.LOOP));
		top.add(Box.createHorizontalGlue());
		
		add(top);
		add(Box.createVerticalStrut(5));
		add(scroll);
		add(Box.createVerticalGlue());
		refresh();
	}

	void edit() {
		Song song = JudahZone.getSong(); 
		if (song == null || song.getChordpro() == null || song.getChordpro().isBlank())
			return;
		try {
			Desktop.getDesktop().open(ChordTrack.fromSong(song));
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
			String text ="<html>" + chord.getChord();
			text += "<BR>";
			text += chord.getLyrics() == null || chord.getLyrics().isBlank() ? "  " : chord.getLyrics();  
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
		JToggleButton loop = new JToggleButton(" 🔁 ");
		JLabel bar = new JLabel("1");
		JPanel left = new JPanel();
		JPanel lbls = new JPanel();

		Click total = new Click("of 0", e->length());

		Group(Section s) {
			groups.add(this);
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			section = s;
			total.setText(" of " + chords.bars(section.getCount()) + " ");
			loop.setSelected(section.isOnLoop());
			loop.addActionListener(e->section.toggle(Directive.LOOP));

			lbls.setOpaque(true);
			lbls.add(new JLabel("Bar")); lbls.add(bar); lbls.add(total);

			Gui.resize(left, new Dimension(135, 60));
			left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));
			left.add(lbls);
			left.add(loop);
			left.add(Box.createVerticalGlue());
			
			add(left);
			add(createContent());
			setBorder(BorderFactory.createTitledBorder(s.getName()));
			content.add(this);
			addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					chords.setSection(section, true);
				};
			});
		}

		JPanel createContent() {
			JPanel result = new JPanel(new GridLayout(0, TABS, 8, 10));
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
			
			return result;
		}
		
		void update() {
			bar.setText("" + (chords.getBar() + 1));
		}
		
		void highlight(Section s) {
			setBackground(s == section ? BUTTONS : null);
			left.setBackground(getBackground());
			lbls.setBackground(getBackground());
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
