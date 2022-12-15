package net.judah.seq;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import net.judah.api.MidiReceiver;
import net.judah.gui.Size;
import net.judah.util.Constants;

public class MidiMenu extends JPanel implements MidiSize, ActionListener {

	private final MidiTrack track;
	private final TrackList tracks;
	private final JMenuBar menu = new JMenuBar();
//	@Getter private final BarRoom bars;
	private final JComboBox<String> files = new JComboBox<String>(new String[] {"OomPah", "Sleepwalk", "BlueGreen", "TimeX2"});
	private final JComboBox<Cycle> cycle = new JComboBox<>(Cycle.values());
	private final JComboBox<String> instrument;
	private final JComboBox<Bar> current = new JComboBox<>();

	private final JLabel pat = new JLabel();
	private final JLabel state = new JLabel();
	
	public MidiMenu(Rectangle bounds, MidiView view, TrackList tracks) {
		this.track = view.getTrack();
		this.tracks = tracks;
		view.getVelocity().setValue(VELOCITY);
		instrument = new JComboBox<String>(track.getMidiOut().getPatches());
		instrument.addActionListener(e->{
			MidiReceiver r = track.getMidiOut();
			if (r.getProg(track.getCh()) != instrument.getSelectedIndex())
				r.progChange("" + instrument.getSelectedItem(), track.getCh());
		});
		cycle.addActionListener(e->{
			track.getScheduler().setCycle((Cycle)cycle.getSelectedItem());
		});
		Constants.resize(instrument, Size.COMBO_SIZE);
		Constants.resize(files, Size.COMBO_SIZE);
		Constants.resize(current, Size.COMBO_SIZE);
		Constants.resize(cycle, Size.SMALLER_COMBO);
		Constants.resize(view.getGate(), Size.SMALLER_COMBO);

		setBounds(bounds);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createDashedBorder(Color.BLUE));

		buildMenu();
		add(menu);
		add(files);
		add(instrument);
		add(new JLabel("Current:"));
		add(current);
		add(cycle);
		add(state);
		add(pat);
		add(Box.createHorizontalGlue());
		add(new JLabel("Vel."));
		add(view.getVelocity());
		add(new JLabel("Gate"));
		add(view.getGate());
		add(view.getLive());
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
		Situation s = track.getState();
		state.setText(s.previous + "-[ " + s.current  + " ]-" + s.next  + "-" + s.afterNext);
		if (current.getSelectedIndex() != track.getCurrent())
			setCurrent(track.getCurrent());
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
				change.addActionListener(e->tracks.setCurrent(t));
			result.add(change);
		}
		return result;
	}
	private JMenu fileMenu() {
		JMenu result = new JMenu("File");
		JMenuItem create = new JMenuItem("New");
		create.addActionListener(e->track.clear());
		JMenuItem open = new JMenuItem("Open");
		open.addActionListener(e->track.open());
		JMenuItem save = new JMenuItem("Save");
		save.addActionListener(e ->track.save());
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
		newBar.addActionListener(e->track.newBar());
		JMenuItem copy = new JMenuItem("Copy");
		copy.addActionListener(e->track.copy());
		JMenuItem delete = new JMenuItem("Delete");
		delete.addActionListener(e->track.delete());
		result.add(newBar);
		result.add(copy);
		result.add(delete);
		return result;
	}

	private JMenu noteMenu() { 
		JMenu result = new JMenu("Note");

		JMenuItem copy = new JMenuItem("Copy");
		copy.addActionListener(e->{
			
		});
		JMenuItem delete = new JMenuItem("Delete");
		delete.addActionListener(e->{
			
		});

		JMenuItem cc = new JMenuItem("CC");
		cc.addActionListener(e->{
			
		});

		JMenuItem progChange = new JMenuItem("Prog");
		progChange.addActionListener(e->{
			
		});

		result.add(copy);
		result.add(delete);
		result.add(cc);
		result.add(progChange);
		return result;
	}

	private JMenu cueMenu() {
		JMenu cycle = new JMenu("Cue");
		ButtonGroup group = new ButtonGroup();
		int selected = track.getCue().ordinal();
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
		for (Bar b : track)
			current.addItem(b);
		current.setSelectedItem(track.getBar());
		current.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == current)
			track.setCurrent(current.getSelectedIndex());
	}
	
}

//	JMenuItem rename = new JMenuItem("Rename");
//	rename.addActionListener(e->{
//		String name = Constants.inputBox("Pattern Name:");
//    		if (name == null || name.isEmpty()) return;
//    		track.get(track.getState().getCurrent()).setName(name);
//    		fillPatterns();
//	});
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
