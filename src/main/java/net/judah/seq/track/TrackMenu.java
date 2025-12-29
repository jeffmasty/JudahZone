package net.judah.seq.track;

import static net.judah.gui.Size.COMBO_SIZE;
import static net.judah.gui.Size.MEDIUM;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import net.judah.JudahZone;
import net.judah.drumkit.DrumType;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.PlayWidget;
import net.judah.gui.widgets.RecordWidget;
import net.judah.gui.widgets.TrackVol;
import net.judah.seq.MusicBox;
import net.judah.seq.SynthRack;
import net.judah.seq.Transpose;
import net.judah.seq.automation.Automation;
import net.judah.seq.track.Computer.Update;
import net.judahzone.gui.Actionable;
import net.judahzone.gui.Gui;
import net.judahzone.gui.Icons;

public abstract class TrackMenu extends Box implements MouseListener {

	// TODO transfer Scene/Computer
	public static class SendTo extends JMenu {
		public SendTo(MidiTrack source) {
			super("SendTo...");
	    	for (MidiTrack t : source.isDrums() ? JudahZone.getInstance().getDrumMachine().getTracks() : SynthRack.getSynthTracks())
	    		if (t != source)
	    			add(new Actionable(t.getName(), evt->t.load(source)));
		}
	}

	protected final NoteTrack track;
	protected final MusicBox grid;
	protected final ButtonGroup cue = new ButtonGroup();
	protected final ButtonGroup gate = new ButtonGroup();
	protected final JMenuBar menu = new JMenuBar();
	protected final JMenu file;
	protected final JMenu edit = new JMenu("Edit");
	protected final JMenu tools = new JMenu("Tools");
	protected final JMenu cues = new JMenu("Cue");
	protected final JMenu quantization = new JMenu("Quantization");
	private final String a;
	private final String b;
	protected final PlayWidget play;
	protected RecordWidget capture;
	protected final Program program;
	protected final Programmer programmer;
	protected TrackVol velocity;

	public TrackMenu(MusicBox g, Automation auto) {
		super(BoxLayout.X_AXIS);
		track = g.getTrack();
		grid = g;
		file = new JMenu(track.getName());
		a = track.isDrums() ? "Left" : "Top";
		b = track.isDrums() ? "Right" : "Bottom";
		addMouseListener(this);
		play = new PlayWidget(track);
		programmer = new Programmer(track);
		velocity = new TrackVol(track);

		track.getEditor().tools(tools);
		tools.add(new Actionable("Automation", e->auto.init(track)));

		fileSetup();
		fileMenu();
		menu.add(file);
		if (track.isSynth()) {
			menu.add(tools);
			barMenu(edit);
			menu.add(edit);
		}
		else
			barMenu(file); // consolidate for drums (no space)

		add(Box.createHorizontalStrut(2));
		add(play);
		if (track.isSynth()) {
			capture = new RecordWidget(track);
			add(capture);
		}
		add(menu);
		program = new Program(track);
		add(Gui.resize(program, track.isDrums()? MEDIUM : COMBO_SIZE));
        add(new Btn(Icons.SAVE, e->track.save()));
		add(programmer);

		if (track.isDrums())
			file.setFont(Gui.BOLD12);

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

	private void fileSetup() {
		for (Cue c : Cue.values()) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(c.name());
			if (track.getCue() == c)
				item.setSelected(true);
			cue.add(item);
			cues.add(item);
			item.addActionListener(e-> track.setCue(c));
		}
		for (Gate g : Gate.values()) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(g.name());
			if (track.getGate() == g)
				item.setSelected(true);
			gate.add(item);
			quantization.add(item);
			item.addActionListener(e -> track.setGate(g));
		}
	}

	private void fileMenu() {
		file.removeAll();

		if (MainFrame.isBundle()) {
			file.add(new Actionable("Clear", e->track.clear()));
			file.add(new Actionable("Import...", e->new ImportMidi(track)));
			file.add(new Actionable("Export...", e ->track.saveAs()));
		} else {
			file.add(new Actionable("New", e->track.clear()));
			file.add(new Actionable("Open", e->track.load()));
			file.add(new Actionable("Save", e->track.save())); // if not bundle
			file.add(new Actionable("Save As...", e ->track.saveAs()));
		}
		file.add(cues);
		file.add(quantization);
	}

//	/** tools info/res, import, remap, condense, clean */
//	private void tools() {
//
//		JMenu trim = new JMenu("Trim");
//		trim.add(new Actionable("Frame", e->trimFrame()));
//		trim.add(new Actionable(a, e->trimBar(true)));
//		trim.add(new Actionable(b, e->trimBar(false)));
//		JMenu insert = new JMenu("Insert"); // TODO
//		insert.add(new Actionable(a, e->insertBar(true)));
//		insert.add(new Actionable(b, e->insertBar(false)));
//		insert.add(new Actionable("Frame", e->insertFrame()));
//
//		tools.add(new Actionable("Track Info...", e->info()));
//		tools.add(new Actionable("Automation", e->Automation.getInstance().init(track, 0l, AutoMode.All)));
//		tools.add(new SendTo(track));
//		tools.add(insert);
//		tools.add(trim);
//	}

	private void barMenu(JMenu menu) { // try the wings!

		JMenu select = new JMenu("Select");
		select.add(new Actionable("None", e->grid.selectNone()));
		select.add(new Actionable(a, e->grid.selectBar(true)));
		select.add(new Actionable(b, e->grid.selectBar(false)));
		select.add(new Actionable("Window", e->grid.selectFrame()));
		select.add(new Actionable("Track", e->grid.selectArea(0, grid.getTrack().getT().ticks())));

		menu.add(select);
		menu.add(new Actionable("Copy", e-> grid.copy()));
		menu.add(new Actionable("Paste", e->track.getEditor().paste()));
		menu.add(new Actionable("Delete", e->grid.delete()));
		menu.add(new Actionable("Undo", e->track.getEditor().undo()));
		menu.add(new Actionable("Redo", e->track.getEditor().redo()));

		if (track.isDrums()) {
			JMenu type = new JMenu("Drum");
			select.add(type);
			for (DrumType t : DrumType.values()) {
				JMenuItem item = new JMenuItem(t.name());
				type.add(item);
				item.addActionListener(e->{
					int data1 = DrumType.valueOf( ((AbstractButton)e.getSource()).getText()).getData1();
					grid.selectArea(0, track.getT().ticks(), data1, data1);
				});
			}

			menu.add(tools);
		}

		menu.add(new Actionable("Transpose...", e->new Transpose(track, grid)));
	}

//	private void info() { // TODO no resolution if song bundle
//		String result = JOptionPane.showInputDialog(SwingUtilities.getWindowAncestor(this),
//				track.info() + "New Resolution:", track.getResolution());
//		if (result == null) return;
//		try { track.setResolution(Integer.parseInt(result));
//		} catch (NumberFormatException e) { RTLogger.log("Resolution", result + ": " + e.getMessage()); }
//	}

//	private void trimFrame() {
//		long start = track.getFrame() * track.getWindow();
//		long end = start + 2 * track.barTicks;
//		Edit trim = new Edit(Type.TRIM, grid.selectFrame());
//		trim.setOrigin(new Prototype(0, start));
//		trim.setDestination(new Prototype(0, end));
//		track.getEditor().push(trim);
//	}
//
//	private void trimBar(boolean left) {
//		long start = track.getFrame() * track.getWindow();
//		if (!left)
//			start += track.barTicks;
//		long end = start + track.barTicks;
//		Edit trim = new Edit(Type.TRIM, grid.selectBar(left));
//		trim.setOrigin(new Prototype(0, start));
//		trim.setDestination(new Prototype(0, end));
//		track.getEditor().push(trim);
//	}
//
//	private void insertBar(boolean left) {
//		long start = track.getFrame() * track.getWindow();
//		if (!left)
//			start += track.barTicks;
//		long end = start + track.barTicks;
//		Edit ins = new Edit(Type.INS, new ArrayList<MidiPair>());
//		ins.setOrigin(new Prototype(0, start));
//		ins.setDestination(new Prototype(0, end));
//		track.getEditor().push(ins);
//	}
//
//	private void insertFrame() {
//		long start = track.getFrame() * track.getWindow();
//		long end = start + track.getWindow();
//		Edit ins = new Edit(Type.INS, new ArrayList<MidiPair>());
//		ins.setOrigin(new Prototype(0, start));
//		ins.setDestination(new Prototype(0, end));
//		track.getEditor().push(ins);
//	}

	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mouseReleased(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }

	public void update() {
		updateCue();
		program.update();
		fileMenu();
	}

	public void update(Update type) {
		if (Update.PROGRAM == type)
			program.update();
		else if (Update.CUE == type)
			updateCue();
		else if (Update.GATE == type)
			updateGate();
		else if (Update.CYCLE == type)
			programmer.getCycle().update();
		else if (Update.CAPTURE == type && capture != null)
			capture.update();
		else if (Update.PLAY == type)
			play.update();
		else if (Update.CURRENT == type)
			programmer.getCurrent().update();
		else if (Update.LAUNCH == type)
			programmer.liftOff();
		else if (Update.FILE == type)
			programmer.liftOff();
		else if (Update.AMP == type)
			velocity.update();
		else if (Update.EDIT == type)
			programmer.liftOff();
	}

}
