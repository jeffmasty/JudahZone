package net.judah.seq;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;

import lombok.Getter;
import net.judah.api.MidiReceiver;
import net.judah.gui.Gui;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.mixer.Channel;
import net.judah.seq.beatbox.BeatsSize;
import net.judah.seq.beatbox.BeatsTab;
import net.judah.util.RTLogger;
import net.judah.widgets.FxButton;
import net.judah.widgets.Knob;
import net.judah.widgets.TrackFolder;

public class MidiMenu extends JPanel implements BeatsSize, ActionListener, MouseListener{

	private final MidiTrack track;
	private final MidiView view;
	private final TrackList tracks;
	private final MidiTab tab;
	private final JMenuBar menu = new JMenuBar();
	@Getter private final TrackFolder files;
	private final JComboBox<Cycle> cycle = new JComboBox<>(Cycle.values());
	private final JComboBox<String> instrument;
	private final JComboBox<Integer> current = new JComboBox<>();
	private final Knob velocity = new Knob();
	@Getter private final JComboBox<Gate> gate = new JComboBox<>(Gate.values());
	private final JButton playWidget = new JButton("  ▶️  ");
	private final JButton recWidget = new JButton("  ◉  ");
	private final JButton liveWidget = new JButton("  🔁  ");
	private final JButton metroWidget = new JButton(Icons.get("left.png"));
	
	private final JLabel pat = new JLabel();
	private final JLabel state = new JLabel();
	
	public MidiMenu(Rectangle bounds, MidiView view, TrackList tracks, MidiTab tab) {
		this.view = view;
		this.track = view.getTrack();
		this.tracks = tracks;
		this.tab = tab;
		addMouseListener(this);
		instrument = new JComboBox<String>(track.getMidiOut().getPatches());
		instrument.addActionListener(e->{
			MidiReceiver r = track.getMidiOut();
			if (r.getProg(track.getCh()) != instrument.getSelectedIndex())
				r.progChange("" + instrument.getSelectedItem(), track.getCh());
		});
		cycle.addActionListener(e->{
			track.getScheduler().setCycle((Cycle)cycle.getSelectedItem());
		});

		velocity.setValue(Math.round(track.getMidiOut().getAmplification() * 100));
		velocity.addListener(val->track.getMidiOut().setAmplification(val * 0.01f));
		files = new TrackFolder(track);
		
		Gui.resize(instrument, Size.COMBO_SIZE);
		Gui.resize(files, Size.COMBO_SIZE);
		Gui.resize(current, Size.MICRO);
		Gui.resize(cycle, Size.SMALLER_COMBO);
		Gui.resize(gate, Size.SMALLER_COMBO);

		setBounds(bounds);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createDashedBorder(Color.BLUE));

		buildMenu();
		btns();
		add(menu);
		add(files);
		add(instrument);
		add(cycle);
		add(new JLabel("Current"));
		add(current);
		add(state);
		add(pat);
		add(Box.createHorizontalGlue());
		if (track.isPiano()) {
			add(new JLabel("Gate"));
			add(gate);
			gate.setSelectedItem(Gate.SIXTEENTH);
		}
		add(new JLabel("Vel."));
		add(velocity);
		add(new FxButton((Channel)track.getMidiOut()));

		fillPatterns();
		update();
	}
	
	public void setCurrent(int idx) {
		current.removeActionListener(this);
		if (idx < current.getItemCount())
			current.setSelectedIndex(idx);
		current.addActionListener(this);
	}

	public void update() {
		if (cycle.getSelectedIndex() != track.getScheduler().getCycle().ordinal())
			cycle.setSelectedIndex(track.getScheduler().getCycle().ordinal());
		pat.setText(" :" + track.getScheduler().getCount());
		Scheduler s = track.getScheduler();
		state.setText(s.previous + "-[ " + track.getCurrent() + " ]-" + s.next  + "-" + s.afterNext);
		if (current.getSelectedIndex() != track.getCurrent())
			setCurrent(track.getCurrent());
		if (current.getItemCount() != track.bars())
			fillPatterns();
		playWidget.setBackground(track.isActive() ? Pastels.GREEN : null);
		recWidget.setBackground(track.isRecord() ? Pastels.RED : null);
		liveWidget.setBackground(view.isLive() ? Pastels.BLUE : null);
		metroWidget.setIcon(track.getScheduler().getCount() % 2 == 0 ? Icons.get("left.png") : Icons.get("right.png"));
		if (track.isDrums())
			setBorder(tab.getCurrent() == view ? Gui.RED : Gui.NONE);
		velocity.setValue((int) (track.getMidiOut().getAmplification() * 100));
		files.update();
	}

	private void buildMenu() {
		menu.add(traxMenu());
		menu.add(fileMenu());
		menu.add(trackMenu());
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
				change.addActionListener(e-> {
					tracks.setCurrent(t);
					MainFrame.setFocus(tracks);
				});
			result.add(change);
		}
		return result;
	}
	private JMenu fileMenu() {
		JMenu result = new JMenu("File");
		JMenuItem create = new JMenuItem("New");
		create.addActionListener(e->track.clear());
		JMenuItem open = new JMenuItem("Open");
		open.addActionListener(e->track.load());
		JMenuItem save = new JMenuItem("Save");
		save.addActionListener(e ->track.saveAs());
		JMenuItem importMidi = new JMenuItem("Import");
		importMidi.addActionListener(e-> new ImportMidi(track));
		result.add(create);
		result.add(open);
		result.add(save);
		result.add(importMidi);
		return result;
	}
	private JMenu trackMenu() {
		JMenu result = new JMenu("Track");
		result.add(cueMenu());
		result.add(new JMenuItem("Record"));
		result.add(new JMenuItem("MPK"));
		return result;
	}
	private JMenu barMenu() { // try the wings!
		JMenu result = new JMenu("Bar");
		JMenuItem newBar = new JMenuItem("New");
		newBar.addActionListener(e->track.setCurrent(track.bars() + 1));
		JMenuItem copy = new JMenuItem("Copy");
		copy.addActionListener(e->track.copy(track.getCurrent()));
		JMenuItem delete = new JMenuItem("Delete");
		delete.addActionListener(e->track.delete(track.getCurrent()));
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
		int selected = track.getScheduler().getCue().ordinal();
		for (int i = 0; i < Cue.values().length; i++) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(Cue.values()[i].name(), i == selected);
			group.add(item);
			cycle.add(item);
			final int num = i;
			item.addActionListener(e->track.setCue(Cue.values()[num]));
		}
		return cycle;
	}

	public void fillPatterns() {
		current.removeActionListener(this);
		current.removeAll();
		for (int i = 0; i < track.bars(); i++)
			current.addItem(i);
		current.setSelectedItem(track.getCurrent());
		current.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == current)
			track.setCurrent(current.getSelectedIndex());
	}

	public void updateLength() {
		// measure/resolution/steps
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
		
		metroWidget.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				track.getScheduler().setCount(track.getScheduler().getCount() + 1);}});
		add(metroWidget);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) { 
		RTLogger.log(this, "menu pressed ");
		if (track.isDrums())
			((BeatsTab)tab).setCurrent(view);
		update();
	}

	@Override
	public void mouseReleased(MouseEvent e) { }

	@Override
	public void mouseEntered(MouseEvent e) { }

	@Override
	public void mouseExited(MouseEvent e) { }
	
	
}

//	private JMenu cycleMenu() {
//		JMenu cycle = new JMenu("Cycle");
//		ButtonGroup group = new ButtonGroup();
//		int selected = track.getScheduler().getCycle().ordinal();
//		for (int i = 0; i < Plan.CYCLES.length; i++) {
//			JRadioButtonMenuItem item = new JRadioButtonMenuItem(Cycle.values()[i].name(), i == selected);
//			group.add(item);
//			cycle.add(item);
//			final int num = i;
//			item.addActionListener(e->track.getScheduler().setCycle(Cycle.values()[num]));}
//		return cycle;
