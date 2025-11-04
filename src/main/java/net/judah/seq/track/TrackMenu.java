package net.judah.seq.track;

import static net.judah.JudahZone.getSeq;
import static net.judah.gui.Size.COMBO_SIZE;
import static net.judah.gui.Size.MEDIUM_COMBO;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

import net.judah.gui.Actionable;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.PlayWidget;
import net.judah.gui.RecordWidget;
import net.judah.gui.Updateable;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Btn;
import net.judah.omni.Icons;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiPair;
import net.judah.seq.MusicBox;
import net.judah.seq.Prototype;
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
	protected final JMenu tools = new JMenu("Tools");
	private final String a;
	private final String b;


	public TrackMenu(MusicBox g) {
		super(BoxLayout.X_AXIS);
		grid = g;
		track = g.getTrack();
		file = new JMenu(track.getName());
		a = track.isDrums() ? "Left" : "Top";
		b = track.isDrums() ? "Right" : "Bottom";
		addMouseListener(this);

		tools();
		if (track.isDrums())
			file.setFont(Gui.BOLD12);

		menu.add(fileMenu());
		if (track.isSynth()) {
			menu.add(tools);
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
		instances.add(this);
		MainFrame.update(this);
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
		file.add(cues);
		file.add(quantization);
		return file;
	}

	/** tools info/res, import, remap, condense, clean */
	private void tools() {

		JMenu trim = new JMenu("Trim");
		trim.add(new Actionable("Frame", e->trimFrame()));
		trim.add(new Actionable(a, e->trimBar(true)));
		trim.add(new Actionable(b, e->trimBar(false)));
		JMenu insert = new JMenu("Insert"); // TODO
		insert.add(new Actionable(a, e->insertBar(true)));
		insert.add(new Actionable(b, e->insertBar(false)));
		insert.add(new Actionable("Frame", e->insertFrame()));
		// insert.add(new Actionable("Custom", e->trimFrame())); // TODO

		tools.add(new Actionable("Info/Res...", e->resolution()));
		tools.add(new SendTo(track));
		tools.add(insert);
		tools.add(trim);
	}

	private void barMenu(JMenu menu) { // try the wings!

		JMenu select = new JMenu("Select");
		select.add(new Actionable("All", e->grid.selectFrame()));
		select.add(new Actionable(a, e->grid.selectBar(true)));
		select.add(new Actionable(b, e->grid.selectBar(false)));
		select.add(new Actionable("None", e->grid.selectNone()));

		menu.add(select);
		menu.add(new Actionable("Copy", e-> grid.copy()));
		menu.add(new Actionable("Paste", e->grid.paste()));
		menu.add(new Actionable("Delete", e->grid.delete()));
		menu.add(new Actionable("Undo", e->grid.undo()));
		menu.add(new Actionable("Redo", e->grid.redo()));

		if (track.isDrums())
			menu.add(tools);

		menu.add(new Actionable("Transpose...", e->new Transpose(track, grid)));
		// result.add(new Actionable("CC", e->{ }));
		// result.add(new Actionable("Prog", e->{ }));
	}

	private void resolution() {
		String result = JOptionPane.showInputDialog(SwingUtilities.getWindowAncestor(this),
				track.info() + "New Resolution:", track.getResolution());
		if (result == null) return;
		try { track.setResolution(Integer.parseInt(result));
		} catch (NumberFormatException e) { RTLogger.log("Resolution", result + ": " + e.getMessage()); }
	}

	private void trimFrame() {
		long start = track.getFrame() * track.getWindow();
		long end = start + 2 * track.barTicks;
		Edit trim = new Edit(Type.TRIM, grid.selectFrame());
		trim.setOrigin(new Prototype(0, start));
		trim.setDestination(new Prototype(0, end));
		grid.push(trim);
	}

	private void trimBar(boolean left) {
		long start = track.getFrame() * track.getWindow();
		if (!left)
			start += track.barTicks;
		long end = start + track.barTicks;
		Edit trim = new Edit(Type.TRIM, grid.selectBar(left));
		trim.setOrigin(new Prototype(0, start));
		trim.setDestination(new Prototype(0, end));
		grid.push(trim);
	}

	private void insertBar(boolean left) {
		long start = track.getFrame() * track.getWindow();
		if (!left)
			start += track.barTicks;
		long end = start + track.barTicks;
		Edit ins = new Edit(Type.INS, new ArrayList<MidiPair>());
		ins.setOrigin(new Prototype(0, start));
		ins.setDestination(new Prototype(0, end));
		grid.push(ins);
	}

	private void insertFrame() {
		long start = track.getFrame() * track.getWindow();
		long end = start + track.getWindow();
		Edit ins = new Edit(Type.INS, new ArrayList<MidiPair>());
		ins.setOrigin(new Prototype(0, start));
		ins.setDestination(new Prototype(0, end));
		grid.push(ins);
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
