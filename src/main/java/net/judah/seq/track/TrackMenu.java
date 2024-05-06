package net.judah.seq.track;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.*;

import net.judah.JudahZone;
import net.judah.gui.*;
import net.judah.gui.JudahMenu.Actionable;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.ModeCombo;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Arrow;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.GateCombo;
import net.judah.gui.widgets.TrackVol;
import net.judah.midi.JudahClock;
import net.judah.seq.*;
import net.judah.seq.arp.Mode;
import net.judah.seq.beatbox.BeatsSize;
import net.judah.seq.beatbox.BeatsTab;
import net.judah.seq.piano.Octaves;
import net.judah.seq.piano.PianoView;

public class TrackMenu extends JPanel implements BeatsSize, MouseListener {

	private final MidiTrack track;
	private final MidiView view;
	private final TrackList tracks;
	private final MidiTab tab;
	private final JMenuBar menu = new JMenuBar();
	private final ButtonGroup mode = new ButtonGroup();
	private final ButtonGroup cue = new ButtonGroup();
	
	public TrackMenu(Rectangle bounds, MidiView view, TrackList tracks, MidiTab tab) {
		this.view = view;
		this.track = view.getTrack();
		this.tracks = tracks;
		this.tab = tab;
		menu.add(traxMenu());
		menu.add(fileMenu());
		menu.add(barMenu());
		
		setBounds(bounds);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		add(new PlayWidget(track));
		add(menu);
		add(Gui.resize(new Program(track), Size.COMBO_SIZE));
		if (track.isSynth()) 
			add(new ModeCombo((PianoTrack)track));
		add(Gui.resize(new Folder(track), Size.COMBO_SIZE));
        add(new Btn(Icons.SAVE, e->track.save())); 
		add(new Programmer(track));
		add(new Btn(Icons.NEW_FILE, e->track.setCurrent(track.bars() + 1)));
		add(Box.createHorizontalGlue());
		if (track.isSynth()) {
			zoomMenu((PianoView)view);
			add(Box.createHorizontalGlue());
		}
		add(new JLabel("Vel/Gate"));
		add(new TrackVol(track));
		add(Gui.resize(new GateCombo(track), Size.SMALLER_COMBO));
		if (track.isDrums())
        	add(new Btn(Icons.DETAILS_VEW, e->MainFrame.setFocus((((DrumTrack)track).getKit().getGui()))));
		add(new RecordWidget(track));

		update();
		addMouseListener(this);
	}

	public void update() {
		if (track.isDrums())
			setBorder(tab.getCurrent() == view ? Gui.RED : Gui.SUBTLE);
		updateMode();
		updateCue();
	}
	public void updateMode() {
		if (track.isDrums())
			return;
		int i = 0;
		Enumeration<AbstractButton> it = mode.getElements();
		PianoTrack t = (PianoTrack)track;
		while (it.hasMoreElements()) 
			if (t.getArp().getMode().ordinal() == i++) 
				it.nextElement().setSelected(true);
			else 
				it.nextElement();
	}

	public void updateCue() {
		int i = 0;		
		Enumeration<AbstractButton> it = cue.getElements();
		while(it.hasMoreElements())
			if(track.getCue().ordinal() == i++)
				it.nextElement().setSelected(true);
			else
				it.nextElement();
	}
	
	private JMenu traxMenu() {
		JMenu result = new JMenu(track.getName());
		for (MidiTrack t : tracks) {
			JMenuItem change = new JMenuItem(t.getName());
			if (t == track) 
				change.setEnabled(false);
			else
				change.addActionListener(e-> JudahZone.getFrame().edit(t));
			result.add(change);
		}
		return result;
	}
	private JMenu fileMenu() {
		JMenu result = new JMenu("Track");
		result.add(new Actionable("New", e->track.clear()));
		result.add(new Actionable("Open", e->track.load()));
		result.add(new Actionable("Save", e->track.save()));
		result.add(new Actionable("Save As...", e ->track.saveAs()));
		result.add(new Actionable("Import...", e->new ImportMidi(track)));
		result.add(new Actionable("Resolution..", e->MidiTools.resolution(track)));
		JMenu cues = new JMenu("Cue");
		for (Cue c : Cue.values()) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(c.name());
			if (track.getCue() == c)
				item.setSelected(true);
			cue.add(item);
			cues.add(item);
			item.addActionListener(e-> track.setCue(c));
		}
		result.add(cues);
		
		if (track.isSynth()) {
			JMenu modes = new JMenu("Arp");
			for (Mode m : Mode.values()) {
				JRadioButtonMenuItem item = new JRadioButtonMenuItem(m.name());
				if (((PianoTrack)track).getArp().getMode() == m)
					item.setSelected(true);
				modes.add(item);
				mode.add(item);
				item.addActionListener(e-> ((PianoTrack)track).getArp().setMode(m));
			}
			result.add(modes);
		}
		return result;
	}
	private JMenu barMenu() { // try the wings!
		JMenu result = new JMenu("Edit");
		
		JMenu frames = new JMenu("Frame");
		frames.add(new Actionable("New", e->track.setCurrent(track.bars() + 1)));
		frames.add(new Actionable("Copy", e->copyFrame()));
		frames.add(new Actionable("Paste", e->insertFrame()));
		frames.add(new Actionable("Delete", e->deleteFrame()));
		JMenu select = new JMenu("Select");
		select.add(new Actionable("All", e->view.getGrid().selectFrame()));
		select.add(new Actionable("None", e->view.getGrid().selectNone()));

		result.add(new Actionable("Copy", e-> view.getGrid().copy()));
		result.add(new Actionable("Paste", e->view.getGrid().paste()));
		result.add(new Actionable("Delete", e->view.getGrid().delete()));
		result.add(frames);
		result.add(select);
		result.add(new Actionable("Transpose...", e->new Transpose(track, view.getGrid())));
		if (view.getTrack().isSynth())
			result.add(new Actionable("Duration...", e->new Duration(view.getGrid())));
		result.add(new Actionable("Undo", e->view.getGrid().undo()));
		result.add(new Actionable("Redo", e->view.getGrid().redo()));
		// result.add(new Actionable("CC", e->{ }));
		// result.add(new Actionable("Prog", e->{ }));
		// result.add(new Actionable("Chords...", e->{ }));
		return result;
	}
	
	private void zoomMenu(PianoView view) {
		JPanel zoom = new JPanel();
		zoom.setLayout(new BoxLayout(zoom, BoxLayout.LINE_AXIS));
		zoom.setBorder(Gui.SUBTLE);
		zoom.add(Box.createHorizontalStrut(4));
		zoom.add(new Arrow(Arrow.WEST, e->view.tonic(false)));
		zoom.add(new JLabel("Octs"));
		zoom.add(new Octaves(view));
		zoom.add(new Arrow(Arrow.EAST, e->view.tonic(true)));
		zoom.add(Box.createHorizontalStrut(4));
		add(zoom);
	}
	
	private void copyFrame() {
		MusicBox grid = view.getGrid();
		Notes selected = new Notes(grid.getSelected());
		grid.selectFrame();
		grid.copy();
		grid.select(selected);
	}

	// TODO undo/redo
	private void deleteFrame() {
		view.getGrid().selectFrame();
		view.getGrid().delete();
		
		int frame = track.getCurrent() - (JudahClock.isEven() ? 0 : 1);
		long ref = frame * track.barTicks;
		Track t = track.getT();
		MidiEvent e;
		long diff = track.getWindow();
		for (int i = t.size() - 1; i > -1; i--) {
			if (t.get(i).getMessage() instanceof ShortMessage == false)
				continue;
			e = t.get(i);
			if (e.getTick() < ref)
				break;
			e.setTick(e.getTick() - diff);
		}
		view.getGrid().repaint();
	}
	
	// TODO undo/redo
	private void insertFrame() {
		int frame = track.getFrame();
		long ref = frame * track.barTicks;
		long diff = track.getWindow();
		Track t = track.getT();
		MidiEvent e;
		for (int i = t.size() - 1; i > -1; i--) {
			if (t.get(i).getMessage() instanceof ShortMessage == false)
				continue;
			e = t.get(i);
			if (e.getTick() < ref)
				break;
			e.setTick(e.getTick() + diff);
		}
		view.getGrid().paste();
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
