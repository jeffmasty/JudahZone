package net.judah.seq;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.settable.Bar;
import net.judah.gui.settable.Cycle;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Arrow;
import net.judah.gui.widgets.FxButton;
import net.judah.gui.widgets.TrackVol;
import net.judah.mixer.Channel;
import net.judah.seq.beatbox.BeatsSize;
import net.judah.seq.beatbox.BeatsTab;
import net.judah.song.Trigger;

public class MidiMenu extends JPanel implements BeatsSize, MouseListener {

	private final MidiTrack track;
	private final MidiView view;
	private final TrackList tracks;
	private final MidiTab tab;
	private final JMenuBar menu = new JMenuBar();
	@Getter private final Folder files;
	private final Cycle cycle;
	private final Bar frames;
	private final Program progChange;
	private final TrackVol vol;
	@Getter private final JComboBox<Gate> gate = new JComboBox<>(Gate.values());
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
		progChange = new Program(track.getMidiOut(), track.getCh());
		frames = new Bar(track);
		cycle = new Cycle(track);

		vol = new TrackVol(track);
		files = new Folder(track);
		metroWidget.addActionListener(e->track.cycle());
		
		Gui.resize(progChange, Size.COMBO_SIZE);
		Gui.resize(files, Size.COMBO_SIZE);
		Gui.resize(frames, Size.MICRO);
		Gui.resize(cycle, Size.SMALLER_COMBO);
		Gui.resize(gate, Size.SMALLER_COMBO);

		setBounds(bounds);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createDashedBorder(Color.BLUE));

		buildMenu();
		btns();
		add(menu);
		add(files);
		add(progChange);

		
		add(new JLabel("Cycle "));
		add(cycle);

		add(Box.createHorizontalGlue());
		add(new JLabel("Frame "));
		add(new Arrow(Arrow.WEST, e->track.setFrame(track.getFrame() - 1)));
		add(frames);
		add(total);
		add(new Arrow(Arrow.EAST, e->track.setFrame(track.getFrame() + 1)));
		add(metroWidget);
		add(Box.createHorizontalGlue());

		if (track.isSynth()) {
			add(new JLabel("Gate"));
			add(gate);
			gate.setSelectedItem(Gate.SIXTEENTH);
		}
		add(new FxButton((Channel)track.getMidiOut()));
		add(vol);
		add(new JLabel(" Vol "));
		update();
	}
	
	public void update() {
		playWidget.setBackground(track.isActive() ? Pastels.GREEN : null);
		recWidget.setBackground(track.isRecord() ? Pastels.RED : null);
		liveWidget.setBackground(view.isLive() ? Pastels.BLUE : null);
		metroWidget.setIcon(track.isEven() ? Icons.get("left.png") : Icons.get("right.png"));
		if (track.isDrums())
			setBorder(tab.getCurrent() == view ? Gui.RED : Gui.SUBTLE);
		if (track.getAmplification() != vol.getValue())
			vol.setValue(track.getAmplification());
		files.update();
		total.setText("" + track.frames());
	}

	private void buildMenu() {
		menu.add(traxMenu());
		menu.add(fileMenu());
		menu.add(barMenu());
		menu.add(noteMenu());
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
		JMenu result = new JMenu("Track");
		JMenuItem create = new JMenuItem("New");
		create.addActionListener(e->track.clear());
		JMenuItem open = new JMenuItem("Open");
		open.addActionListener(e->track.load());
		JMenuItem save = new JMenuItem("Save");
		save.addActionListener(e->track.save());
		JMenuItem saveAs = new JMenuItem("Save...");
		saveAs.addActionListener(e ->track.saveAs());
		JMenuItem importMidi = new JMenuItem("Import");
		importMidi.addActionListener(e-> new ImportMidi(track));
		result.add(create);
		result.add(open);
		result.add(save);
		result.add(saveAs);
		result.add(importMidi);
		result.add(cueMenu());
		// result.add(new JMenuItem("Record")); // TODO
		// result.add(new JMenuItem("MPK")); // TODO
		return result;
	}
	private JMenu barMenu() { // try the wings!
		JMenu result = new JMenu("Frame");
		JMenuItem newBar = new JMenuItem("New");
		newBar.addActionListener(e->track.setFrame(track.frames() + 1));
		JMenuItem copy = new JMenuItem("Copy");
		copy.addActionListener(e->track.copyFrame(track.getFrame()));
		JMenuItem delete = new JMenuItem("Delete");
		delete.addActionListener(e->track.deleteFrame(track.getFrame()));
		result.add(newBar);
		result.add(copy);
		result.add(delete);
		return result;
	}

	private JMenu noteMenu() { 
		JMenu result = new JMenu("Note");

		JMenuItem copy = new JMenuItem("Copy");
		copy.addActionListener(e->{
			view.getGrid().getMusician().copy();
		});
		
		JMenuItem paste = new JMenuItem("Paste");
		paste.addActionListener(e->{
			view.getGrid().getMusician().paste();
		});
		JMenuItem delete = new JMenuItem("Delete");
		delete.addActionListener(e->{
			view.getGrid().getMusician().delete();
		});

		JMenuItem cc = new JMenuItem("CC");
		cc.addActionListener(e->{
			
		});

		JMenuItem progChange = new JMenuItem("Prog");
		progChange.addActionListener(e->{
			
		});

		result.add(copy);
		result.add(paste);
		result.add(delete);
		result.add(cc);
		result.add(progChange);
		return result;
	}

	private JMenu cueMenu() {
		JMenu cycle = new JMenu("Cue");
		ButtonGroup group = new ButtonGroup();
		int selected = track.getCue().ordinal();
		for (int i = 0; i < Trigger.values().length; i++) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(Trigger.values()[i].name(), i == selected);
			group.add(item);
			cycle.add(item);
			final int num = i;
			item.addActionListener(e->track.setCue(Trigger.values()[num]));
		}
		return cycle;
	}

	private void btns() {
		playWidget.addMouseListener(new MouseAdapter() { 
			@Override public void mouseClicked(MouseEvent e) {
			if (track.isActive() || track.isOnDeck()) 
				track.setActive(false);
			else track.setActive(true);}});
		playWidget.setOpaque(true);
		add(playWidget);
		
		recWidget.addMouseListener(new MouseAdapter() { 
			@Override public void mouseClicked(MouseEvent e) {
			track.setRecord(!track.isRecord());
		}});
		recWidget.setOpaque(true);
		add(recWidget);

		liveWidget.addActionListener(e-> {
			view.setLive(!view.isLive());
			MainFrame.update(track);
		});
		liveWidget.setOpaque(true);
		add(liveWidget);
		
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
