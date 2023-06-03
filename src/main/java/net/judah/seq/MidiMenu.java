package net.judah.seq;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;

import javax.swing.*;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.Icons;
import net.judah.gui.JudahMenu.Actionable;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.settable.*;
import net.judah.gui.widgets.Arrow;
import net.judah.gui.widgets.FxButton;
import net.judah.gui.widgets.GateCombo;
import net.judah.gui.widgets.TrackVol;
import net.judah.midi.MpkTranspose;
import net.judah.mixer.Channel;
import net.judah.seq.beatbox.BeatsSize;
import net.judah.seq.beatbox.BeatsTab;

public class MidiMenu extends JPanel implements BeatsSize, MouseListener {

	private final MidiTrack track;
	private final MidiView view;
	private final TrackList tracks;
	private final MidiTab tab;
	private final JMenuBar menu = new JMenuBar();
	private final GateCombo gate;
	private final Folder files;
	private final Cycle cycle;
	private final Launch launch;
	private final Bar frames;
	private final Program progChange;
	@Getter private final TrackVol vol;
	private final JButton playWidget = new JButton(" ▶️ ");
	private final Recorder record;
	private final MpkTranspose transpose;
	private final JButton metroWidget = new JButton(Icons.get("left.png"));
	private final JLabel total = new JLabel();
	private ButtonGroup mode;
	
	public MidiMenu(Rectangle bounds, MidiView view, TrackList tracks, MidiTab tab) {
		this.view = view;
		this.track = view.getTrack();
		this.tracks = tracks;
		this.tab = tab;
		
		setBounds(bounds);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createDashedBorder(Color.BLUE));

		record = track.getRecorder();
		transpose = track.getTransposer();
		progChange = new Program(track.getMidiOut(), track.getCh());
		frames = new Bar(track);
		cycle = new Cycle(track);
		launch = new Launch(track);
		vol = new TrackVol(track);
		files = new Folder(track);
		metroWidget.addActionListener(e->track.cycle());
		CueCombo cue = new CueCombo(track);
		playWidget.addActionListener(e->track.trigger());
		playWidget.setOpaque(true);
		gate = new GateCombo(track);

		Gui.resize(cue, Size.SMALLER_COMBO);
		Gui.resize(files, Size.COMBO_SIZE);
		Gui.resize(cycle, Size.SMALLER_COMBO);
		Gui.resize(gate, Size.SMALLER_COMBO);
		Gui.resize(frames, Size.MICRO);
		Gui.resize(launch, Size.MICRO);
		Gui.resize(progChange, Size.COMBO_SIZE);

		add(playWidget);
		add(cue);
		menu.add(traxMenu());
		menu.add(fileMenu());
		menu.add(barMenu());
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
		add(new JLabel(" Vol"));
		add(vol);
		add(new FxButton((Channel)track.getMidiOut()));
		add(record);
		add(transpose);
		add(progChange);

		update();
		addMouseListener(this);

	}

	public void update() {
		playWidget.setBackground(track.isActive() ? Pastels.GREEN : track.isOnDeck() ? track.getCue().getColor() : null);
		record.update();
		transpose.update();
		metroWidget.setIcon(track.isEven() ? Icons.get("left.png") : Icons.get("right.png"));
		if (track.isDrums())
			setBorder(tab.getCurrent() == view ? Gui.RED : Gui.SUBTLE);
		total.setText("" + track.frames());
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
		frames.add(new Actionable("New", e->track.setFrame(track.frames() + 1)));
		frames.add(new Actionable("Copy", e->track.copyFrame(track.getFrame())));
		frames.add(new Actionable("Delete", e->track.deleteFrame(track.getFrame())));
		JMenu select = new JMenu("Select");
		select.add(new Actionable("All", e->view.getGrid().selectFrame()));
		select.add(new Actionable("None", e->view.getGrid().selectNone()));
		
		result.add(new Actionable("Copy", e-> view.getGrid().copy()));
		result.add(new Actionable("Paste", e->view.getGrid().paste()));
		result.add(new Actionable("Delete", e->view.getGrid().delete()));
		result.add(frames);
		result.add(select);
		result.add(new Actionable("Transpose...", e->new Transpose(view.getGrid())));
		if (view.getTrack().isSynth())
			result.add(new Actionable("Duration...", e->new Duration(view.getGrid())));
		result.add(new Actionable("Undo", e->view.getGrid().undo()));
		result.add(new Actionable("Redo", e->view.getGrid().redo()));
		// result.add(new Actionable("CC", e->{ }));
		// result.add(new Actionable("Prog", e->{ }));
		// result.add(new Actionable("Chords...", e->{ }));

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
