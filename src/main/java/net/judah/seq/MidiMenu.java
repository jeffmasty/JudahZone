package net.judah.seq;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.Icons;
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
		
		recWidget.addActionListener(e->track.setRecording(!track.isRecording()));
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
		if (track.isSynth()) {
			add(new JLabel("Gate"));
			add(gate);
			gate.setSelectedItem(Gate.SIXTEENTH);
		}
		add(new FxButton((Channel)track.getMidiOut()));
		add(vol);
		add(new JLabel(" Vol "));
		add(recWidget);
		add(liveWidget);
		add(progChange);

		update();
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
		JMenuItem create = new JMenuItem("New");
		create.addActionListener(e->track.clear());
		JMenuItem open = new JMenuItem("Open MIDI");
		open.addActionListener(e->track.load());
		JMenuItem save = new JMenuItem("Save MIDI");
		save.addActionListener(e->track.save());
		JMenuItem saveAs = new JMenuItem("Save As...");
		saveAs.addActionListener(e ->track.saveAs());
		JMenuItem importMidi = new JMenuItem("Import MIDI");
		importMidi.addActionListener(e-> new ImportMidi(track));
		result.add(save);
		result.add(saveAs);
		result.add(create);
		result.add(open);
		result.add(importMidi);
		// result.add(new JMenuItem("Record")); // TODO
		// result.add(new JMenuItem("MPK")); // TODO
		return result;
	}
	private JMenu barMenu() { // try the wings!
		JMenu result = new JMenu("Edit");
		JMenuItem newBar = new JMenuItem("New Frame");
		newBar.addActionListener(e->track.setFrame(track.frames() + 1));
		JMenuItem copy = new JMenuItem("Copy Frame");
		copy.addActionListener(e->track.copyFrame(track.getFrame()));
		JMenuItem delete = new JMenuItem("Delete Frame");
		delete.addActionListener(e->track.deleteFrame(track.getFrame()));
		result.add(newBar);
		result.add(copy);
		result.add(delete);

		JMenuItem copyNote = new JMenuItem("Copy Notes");
		copyNote.addActionListener(e->{
			view.getGrid().getMusician().copy();
		});
		
		JMenuItem pasteNote = new JMenuItem("Paste Notes");
		pasteNote.addActionListener(e->{
			view.getGrid().getMusician().paste();
		});
		JMenuItem deleteNote = new JMenuItem("Delete Notes");
		delete.addActionListener(e->{
			view.getGrid().getMusician().delete();
		});

		JMenuItem cc = new JMenuItem("CC");
		cc.addActionListener(e->{
			
		});

		JMenuItem progChange = new JMenuItem("Prog");
		progChange.addActionListener(e->{
			
		});

		result.add(copyNote);
		result.add(pasteNote);
		result.add(deleteNote);
//		result.add(cc);
//		result.add(progChange);
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
