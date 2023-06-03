package net.judah.seq.chords;

import static net.judah.gui.Pastels.*;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.*;

import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.settable.ChordProFiles;
import net.judah.gui.widgets.Btn;
import net.judah.song.Song;

public class ChordSheet extends JPanel {
	private static final int TABS = 4;
	private int measure;
	private final ChordTrack chords;
	JScrollPane scroll = new JScrollPane();
	JPanel content;
	JPanel top = new JPanel();
	JLabel section = new JLabel("      ");
	JLabel bar = new JLabel();
	JLabel total = new JLabel();
	
	ArrayList<Crd> parts = new ArrayList<>();
	ChordProFiles file = new ChordProFiles();
	Btn play;
	private final JComboBox<Key> key = new JComboBox<>(Key.values());
	private final JComboBox<Scale> scale = new JComboBox<>(Scale.values());

	
	public ChordSheet(ChordTrack chrds) {
		this.chords = chrds;
		setName("Chords");
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setAutoscrolls(true);
		Gui.resize(scroll, new Dimension(Size.WIDTH_TAB - 200, Size.HEIGHT_TAB - 100));
		play = new Btn("▶️ Chords", e-> chords.toggle());
		play.setOpaque(true);

		top.add(play);
		top.add(file);
		top.add(section);
		top.add(new JLabel("Bar"));
		top.add(bar);
		top.add(new JLabel("of"));
		top.add(total);
		
		top.add(Gui.resize(key, Size.MICRO));
		top.add(Gui.resize(scale, Size.COMBO_SIZE));
		scale.addActionListener(e->JudahZone.getCurrent().setScale((Scale) scale.getSelectedItem()));
		key.addActionListener(e->JudahZone.getCurrent().setKey((Key) key.getSelectedItem()));
		
		add(Box.createVerticalGlue());
		add(top);
		add(Box.createVerticalStrut(10));
		add(scroll);
		add(Box.createVerticalGlue());
		refresh();
	}

	public void update(Chord chord) {
		for (Crd crd : parts) {
			if (crd.getBackground() == Pastels.BLUE)
				if (crd.chord != chord)
					crd.setBackground(null);
			if (crd.chord == chord) {
				crd.scrollRectToVisible(crd.getBounds());			
				crd.setBackground(Pastels.BLUE);
			}
		}
		section();
		play.setBackground(chords.isActive() ? GREEN : chords.isOnDeck() ? YELLOW : null);
		scroll.repaint();
	}
	
	JPanel createContent(Section s) {
		JPanel result = new JPanel(new GridLayout(0, TABS, 8, 10));
		JLabel onDeck = null;
		for (Chord chord : s) {
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
		result.setBorder(BorderFactory.createTitledBorder(s.getName()));
		return result;
	}
	
	public void refresh() {
		section();
		parts.clear();
		scroll.getViewport().removeAll();
		content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
		content.setOpaque(true);
		measure = chords.getClock().getSteps();
		for (Section s : chords.getSections())
			content.add(createContent(s));
		
		scroll.setViewportView(content);
		Song song = JudahZone.getCurrent();
		if (song == null) return;
		if (file.getSelectedItem() != song.getChordpro())
			file.setSelectedItem(song.getChordpro());
		if (song.getKey() != null && key.getSelectedItem() != song.getKey())
			key.setSelectedItem(song.getKey());
		if (song.getScale() != null && scale.getSelectedItem() != song.getScale())
			scale.setSelectedItem(song.getScale());		
	}

	public void section() {
		Section part = chords.getSection();
		if (part == null || part.getName() == null || part.getName().isBlank())
			section.setText("Main");
		else 
			section.setText(section.getName());
		bar.setText("" + (chords.getBar() + 1));
		if (part != null)
			total.setText("" + chords.bars(part.steps()));
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
			parts.add(this);
			addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent me) {
					chords.setCurrent(chord);}});
			}
	}
	

}
