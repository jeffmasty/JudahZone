package net.judah.seq.track;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.*;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.drumkit.DrumKit;
import net.judah.gui.Gui;
import net.judah.gui.JudahMenu.Actionable;
import net.judah.gui.MainFrame;
import net.judah.gui.PlayWidget;
import net.judah.gui.Size;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.ModeCombo;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.CueCombo;
import net.judah.gui.widgets.FxButton;
import net.judah.gui.widgets.GateCombo;
import net.judah.gui.widgets.TrackAmp;
import net.judah.midi.JudahClock;
import net.judah.midi.Panic;
import net.judah.mixer.Channel;
import net.judah.seq.*;
import net.judah.seq.arp.Mode;
import net.judah.seq.beatbox.BeatsSize;
import net.judah.seq.beatbox.BeatsTab;
import net.judah.synth.JudahSynth;
import net.judah.util.Constants;

public class TrackMenu extends JPanel implements BeatsSize, MouseListener {

	private final MidiTrack track;
	private final MidiView view;
	private final TrackList tracks;
	private final MidiTab tab;
	private final JMenuBar menu = new JMenuBar();
	private final GateCombo gate;
	private final Folder files;
	private final Program progChange;
	@Getter private final TrackAmp vol;
	private ButtonGroup mode;
	
	public TrackMenu(Rectangle bounds, MidiView view, TrackList tracks, MidiTab tab) {
		this.view = view;
		this.track = view.getTrack();
		this.tracks = tracks;
		this.tab = tab;
		
		setBounds(bounds);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createDashedBorder(Color.BLUE));

		progChange = new Program(track.getMidiOut(), track.getCh());
		vol = new TrackAmp(track);
		files = new Folder(track);
		gate = new GateCombo(track);

		Gui.resize(files, Size.COMBO_SIZE);
		Gui.resize(gate, Size.SMALLER_COMBO);
		Gui.resize(progChange, Size.COMBO_SIZE);

		menu.add(traxMenu());
		menu.add(fileMenu());
		menu.add(barMenu());

		add(new PlayWidget(track));
		add(menu);
		add(files);
		add(progChange);
		if (track.isSynth()) {
			add(new JLabel("  Arp"));
			add(new ModeCombo(track));
		}
		add(new Programmer(track));
		add(Box.createHorizontalGlue());
		if (track.isDrums()) add(new JLabel("Cue")); // space considerations
		add(new CueCombo(track));
		if (track.isDrums()) add(new JLabel("Gate")); 
		add(gate);
		add(vol);
		
		add(new FxButton((Channel)track.getMidiOut()));
		add(new Btn(UIManager.getIcon("FileChooser.detailsViewIcon"), 
				e->MainFrame.setFocus(JudahZone.getSeq().getKnobs(track))));
        if (track.isDrums())
        	add(new Btn("Kit", e->MainFrame.setFocus(JudahZone.getDrumMachine().getKnobs((DrumKit)track.getMidiOut()))));
        else {
	        if (track.getMidiOut() == JudahZone.getSynth1() || track.getMidiOut() == JudahZone.getSynth2())
	        	add(new Btn("DCO", e->MainFrame.setFocus(((JudahSynth)track.getMidiOut()).getSynthKnobs())));
			add(new Btn(" ! ", e->Constants.execute(new Panic(track))));
        }
        add(new Btn(UIManager.getIcon("FileView.floppyDriveIcon"), e->track.setCurrent(track.bars() + 1))); 
		add(new Btn(UIManager.getIcon("FileView.fileIcon"), e->track.setCurrent(track.bars() + 1)));
		
		update();
		addMouseListener(this);

	}

	public void update() {
		if (track.isDrums())
			setBorder(tab.getCurrent() == view ? Gui.RED : Gui.SUBTLE);
		if (mode != null) {
			int i = 0;
			Enumeration<AbstractButton> it = mode.getElements();
			while (it.hasMoreElements()) 
				if (track.getArp() == null || track.getArp().getMode().ordinal() == i++) 
					it.nextElement().setSelected(true);
				else 
					it.nextElement();
		}
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
		if (track.isSynth()) {
			JMenu modes = new JMenu("Mode");
			mode = new ButtonGroup();
			for (Mode m : Mode.values()) {
				JRadioButtonMenuItem item = new JRadioButtonMenuItem(m.name());
				if (track.getArp().getMode() == m)
					item.setSelected(true);
				modes.add(item);
				mode.add(item);
				item.addActionListener(e-> track.getArp().setMode(m));
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
		int frame = track.getCurrent() - (JudahClock.isEven() ? 0 : 1);

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
