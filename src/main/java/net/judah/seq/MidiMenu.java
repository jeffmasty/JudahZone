package net.judah.seq;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.Icons;
import net.judah.gui.JudahMenu.Actionable;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.settable.*;
import net.judah.gui.widgets.Arrow;
import net.judah.gui.widgets.FxButton;
import net.judah.gui.widgets.TrackVol;
import net.judah.mixer.Channel;
import net.judah.seq.beatbox.BeatsSize;
import net.judah.seq.beatbox.BeatsTab;

public class MidiMenu extends JPanel implements BeatsSize, MouseListener {

	private final MidiTrack track;
	private final MidiView view;
	private final TrackList tracks;
	private final MidiTab tab;
	private final JMenuBar menu = new JMenuBar();
	@Getter private final JComboBox<Gate> gate = new JComboBox<>(Gate.values());
	private final Folder files;
	private final Cycle cycle;
	private final Launch launch;
	private final Bar frames;
	private final Program progChange;
	private final TrackVol vol;
	private final JButton playWidget = new JButton(" ▶️ ");
	private final JButton recWidget = new JButton(" ◉ ");
	private final JButton liveWidget = new JButton(" 🔁 ");
	private final JButton metroWidget = new JButton(Icons.get("left.png"));
	private final JLabel total = new JLabel();
	
	public MidiMenu(Rectangle bounds, MidiView view, TrackList tracks, MidiTab tab) {
		this.view = view;
		this.track = view.getTrack();
		this.tracks = tracks;
		this.tab = tab;
		
		addMouseListener(this);
		setBounds(bounds);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createDashedBorder(Color.BLUE));

		progChange = new Program(track.getMidiOut(), track.getCh());
		frames = new Bar(track);
		cycle = new Cycle(track);
		launch = new Launch(track);
		vol = new TrackVol(track);
		files = new Folder(track);
		metroWidget.addActionListener(e->track.cycle());
		Cue cue = new Cue(track);
		playWidget.addActionListener(e->track.trigger());
		playWidget.setOpaque(true);
		
		recWidget.addActionListener(e->record());
		recWidget.setOpaque(true);

		liveWidget.addActionListener(e-> {
			track.setLive(!track.isLive());
			MainFrame.update(track);
		});
		liveWidget.setOpaque(true);

		Gui.resize(cue, Size.SMALLER_COMBO);
		Gui.resize(files, Size.COMBO_SIZE);
		Gui.resize(cycle, Size.SMALLER_COMBO);
		Gui.resize(gate, Size.SMALLER_COMBO);
		Gui.resize(frames, Size.MICRO);
		Gui.resize(launch, Size.MICRO);
		Gui.resize(progChange, Size.COMBO_SIZE);

		add(playWidget);
		add(cue);
		buildMenu();
		add(menu);
		add(files);
		add(new JLabel(" Cycle "));
		add(cycle);
		add(Box.createHorizontalGlue());
		add(metroWidget);
		add(new JLabel(" Init "));
		add(launch);
		add(new JLabel("of "));
		add(total);
		add(new JLabel(" "));
		add(new Arrow(Arrow.WEST, e->track.setFrame(track.getFrame() - 1)));
		add(frames);
		add(new Arrow(Arrow.EAST, e->track.setFrame(track.getFrame() + 1)));
		add(Box.createHorizontalGlue());
		add(new JLabel("Gate"));
		add(gate);
		gate.setSelectedItem(Gate.SIXTEENTH);
		add(new FxButton((Channel)track.getMidiOut()));
		add(vol);
		add(new JLabel(" Vol "));
		add(recWidget);
		add(liveWidget);
		add(progChange);

		update();
	}
	
	private void record() {
		track.setRecording(!track.isRecording());
		if (track.isRecording())
			JudahZone.getMidi().setKeyboardSynth(track);
	}

	public void update() {
		playWidget.setBackground(track.isActive() ? Pastels.GREEN : track.isOnDeck() ? track.getCue().getColor() : null);
		recWidget.setBackground(track.isRecording() ? Pastels.RED : null);
		liveWidget.setBackground(track.isLive() ? Pastels.BLUE : null);
		metroWidget.setIcon(track.isEven() ? Icons.get("left.png") : Icons.get("right.png"));
		if (track.isDrums())
			setBorder(tab.getCurrent() == view ? Gui.RED : Gui.SUBTLE);
		total.setText("" + track.frames());
	}

	private void buildMenu() {
		menu.add(traxMenu());
		menu.add(fileMenu());
		menu.add(barMenu());
	}
	
	private JMenu traxMenu() {
		JMenu result = new JMenu(track.getName());
		for (MidiTrack t : tracks) {
			JMenuItem change = new JMenuItem(t.getName());
			if (t == track) 
				change.setEnabled(false);
			else
				change.addActionListener(e-> tracks.setCurrent(t));
			result.add(change);
		}
		return result;
	}
	private JMenu fileMenu() {
		JMenu result = new JMenu("File");
		result.add(new Actionable("New", e->track.clear()));
		result.add(new Actionable("Open MIDI", e->track.load()));
		result.add(new Actionable("Save MIDI", e->track.save()));
		result.add(new Actionable("Save As...", e ->track.saveAs()));
		result.add(new Actionable("Import Midi", e->new ImportMidi(track)));
		// result.add(new JMenuItem("Record")); // TODO
		// result.add(new JMenuItem("MPK")); // TODO
		return result;
	}
	private JMenu barMenu() { // try the wings!
		JMenu result = new JMenu("Edit");
		result.add(new Actionable("New Frame", e->track.setFrame(track.frames() + 1)));
		result.add(new Actionable("Copy Frame", e->track.copyFrame(track.getFrame())));
		result.add(new Actionable("Delete Frame", e->track.deleteFrame(track.getFrame())));
		
		result.add(new Actionable("Copy Notes", e-> view.getGrid().copy()));
		result.add(new Actionable("Paste Notes", e->view.getGrid().paste()));
		result.add(new Actionable("Delete Notes",e->view.getGrid().delete()));
		// result.add(new Actionable("CC", e->{ }));
		// result.add(new Actionable("Prog", e->{ }));
		return result;
	}

	@Override
	public void mousePressed(MouseEvent e) { 
		if (track.isDrums())
			((BeatsTab)tab).setCurrent(view);
		update();
	}
	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mouseReleased(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }
	
}
