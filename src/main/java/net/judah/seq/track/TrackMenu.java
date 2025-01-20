package net.judah.seq.track;

import static net.judah.JudahZone.getSeq;
import static net.judah.gui.Size.COMBO_SIZE;
import static net.judah.gui.Size.MEDIUM_COMBO;

import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

import net.judah.JudahZone;
import net.judah.gui.Actionable;
import net.judah.gui.Gui;
import net.judah.gui.PlayWidget;
import net.judah.gui.RecordWidget;
import net.judah.gui.Updateable;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Btn;
import net.judah.omni.Icons;
import net.judah.seq.MusicBox;
import net.judah.seq.Notes;
import net.judah.seq.Transpose;
import net.judah.util.RTLogger;

public abstract class TrackMenu extends Box implements MouseListener, Updateable {

	private static final ArrayList<TrackMenu> instances = new ArrayList<TrackMenu>();
	// TODO transfer Scene/Computer
	public static class SendTo extends JMenu {
		public SendTo(MidiTrack source) {
			super("SendTo...");
	    	for (MidiTrack t : source.isDrums() ? getSeq().getDrumTracks() : getSeq().getSynthTracks())
	    		if (t != source)
	    			add(new Actionable(t.getName(), evt->t.load(source)));
		}
	}

	protected final MidiTrack track;
	protected final MusicBox grid;
	protected final ButtonGroup cue = new ButtonGroup();
	protected final ButtonGroup gate = new ButtonGroup();
	protected final JMenuBar menu = new JMenuBar();
	protected final JMenu file;
	protected final JMenu edit = new JMenu("Edit");

	public TrackMenu(MusicBox g) {
		super(BoxLayout.X_AXIS);
		instances.add(this);
		grid = g;
		track = g.getTrack();
		addMouseListener(this);
		file = new JMenu(track.getName());
		if (track.isDrums())
			file.setFont(Gui.BOLD12);

		menu.add(fileMenu());
		if (track.isSynth()) {
			barMenu(edit);
			menu.add(edit);
		}
		else
			barMenu(file); // consolidate for drums (no space)

		add(Box.createHorizontalStrut(2));
		add(new PlayWidget(track));
		if (track.isSynth())
			add(new RecordWidget(track));
		add(menu);
		add(Gui.resize(new Program(track), track.isSynth()? COMBO_SIZE : MEDIUM_COMBO));
		add(Gui.resize(new Folder(track), track.isSynth()? COMBO_SIZE : MEDIUM_COMBO));
        add(new Btn(Icons.SAVE, e->track.save()));
		add(new Programmer(track));
		EventQueue.invokeLater(() -> update());
	}

	public final void updateCue() {
		int i = 0;
		Enumeration<AbstractButton> it = cue.getElements();
		while(it.hasMoreElements())
			if(track.getCue().ordinal() == i++)
				it.nextElement().setSelected(true);
			else
				it.nextElement();
	}
	public final void updateGate() {
		int i = 0;
		Enumeration<AbstractButton> it = gate.getElements();
		while(it.hasMoreElements())
			if(track.getGate().ordinal() == i++)
				it.nextElement().setSelected(true);
			else
				it.nextElement();
	}

	private JMenu fileMenu() {
		JMenu cues = new JMenu("Cue");
		for (Cue c : Cue.values()) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(c.name());
			if (track.getCue() == c)
				item.setSelected(true);
			cue.add(item);
			cues.add(item);
			item.addActionListener(e-> track.setCue(c));
		}
		JMenu quantization = new JMenu("Quantization");
		for (Gate g : Gate.values()) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(g.name());
			if (track.getGate() == g)
				item.setSelected(true);
			gate.add(item);
			quantization.add(item);
			item.addActionListener(e -> track.setGate(g));
		}

		file.add(new Actionable("New", e->track.clear()));
		file.add(new Actionable("Open", e->track.load()));
		file.add(new Actionable("Save", e->track.save()));
		file.add(new Actionable("Save As...", e ->track.saveAs()));
		file.add(new Actionable("Import...", e->new ImportMidi(track)));
		file.add(new Actionable("Info/Res...", e->resolution()));
		file.add(new SendTo(track));
		file.add(cues);
		file.add(quantization);
		return file;
	}

	private void barMenu(JMenu menu) { // try the wings!
		JMenu frames = new JMenu("Frame");
		frames.add(new Actionable("New", e->track.setCurrent(track.bars() + 1)));
		frames.add(new Actionable("Copy", e->copyFrame()));
		frames.add(new Actionable("Paste", e->insertFrame()));
		frames.add(new Actionable("Delete", e->deleteFrame()));
		JMenu select = new JMenu("Select");
		select.add(new Actionable("All", e->grid.selectFrame()));
		select.add(new Actionable("None", e->grid.selectNone()));
		JMenu notes = new JMenu("Notes");
		notes.add(new Actionable("Copy", e-> grid.copy()));
		notes.add(new Actionable("Paste", e->grid.paste()));
		notes.add(new Actionable("Delete", e->grid.delete()));

		menu.add(new Actionable("Undo", e->grid.undo()));
		menu.add(new Actionable("Redo", e->grid.redo()));
		menu.add(notes);
		menu.add(frames);
		menu.add(select);
		menu.add(new Actionable("Transpose...", e->new Transpose(track, grid)));
		// result.add(new Actionable("CC", e->{ }));
		// result.add(new Actionable("Prog", e->{ }));
	}

	private void resolution() {
		String result = JOptionPane.showInputDialog(track.info() + "New Resolution:", track.getResolution());
		if (result == null) return;
		try { track.setResolution(Integer.parseInt(result));
		} catch (NumberFormatException e) { RTLogger.log("Resolution", result + ": " + e.getMessage()); }
	}

	private void copyFrame() {
		Notes selected = new Notes(grid.getSelected());
		grid.selectFrame();
		grid.copy();
		grid.select(selected);
	}

	// TODO undo/redo
	private void deleteFrame() {
		grid.selectFrame();
		grid.delete();

		int frame = track.getCurrent() - (JudahZone.getClock().isEven() ? 0 : 1);
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
		grid.repaint();
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
		grid.paste();
	}

	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mouseReleased(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }

	public static void updateGate(MidiTrack t) {
		for (TrackMenu m : instances)
			if (m.track == t)
				m.updateGate();
	}
	public static void updateCue(MidiTrack track) {
		for (TrackMenu m : instances)
			if (m.track == track)
				m.updateCue();
	}

//	private void detach(final PianoTrack piano) {
//		TrackList<PianoTrack> temp = new TrackList<PianoTrack>();
//		temp.add(piano);
//		PianoTab frame = new PianoTab(temp);
//		frame.setName(temp.getCurrent().getName());
//		new Detach(frame, Size.TAB_SIZE);

}
