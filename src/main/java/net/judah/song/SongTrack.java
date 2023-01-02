package net.judah.song;

import static javax.swing.SwingConstants.CENTER;
import static net.judah.gui.Pastels.GREEN;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.api.MidiReceiver;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.seq.Cycle;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;
import net.judah.widgets.Btn;
import net.judah.widgets.Knob;
import net.judah.widgets.TrackFolder;

@Getter // TODO MouseWheel listener -> change pattern? 
public class SongTrack extends JPanel implements Size {

	private final MidiTrack track;
	private final Seq seq;
	private Sched state = new Sched();
	
	private final JButton play;
	private final Knob velocity = new Knob();
	private final JComboBox<Cycle> cycle = new JComboBox<Cycle>(Cycle.values());
	private final JComboBox<Integer> launch = new JComboBox<>();
	private final JLabel preview = new JLabel("0 | 1", CENTER);
	private final JComboBox<String> progChange;
	private final TrackFolder files;
	private final ActionListener fileListener = new ActionListener() {
			@Override public void actionPerformed(ActionEvent evt) {
				track.load(new File(track.getFolder(), files.getSelectedItem().toString()));}};

	//  namePlay file cycle bar preview preset amp
	public SongTrack(MidiTrack t, Seq seq) {
		this.track = t;
		this.seq = seq;
		play = new Btn("▶️ " + t.getName(), e->track.setActive(!track.isActive()));
		play.setOpaque(true);
		Gui.resize(play, SMALLER_COMBO);
		progChange = new JComboBox<String>(track.getMidiOut().getPatches());
		files = new TrackFolder(track);
		
		Gui.resize(progChange, COMBO_SIZE);
		Gui.resize(files, COMBO_SIZE);
		
		// bars
		for (int i = 0; i < track.bars(); i++)
			launch.addItem(i);
		launch.setSelectedIndex(0);
		
		setOpaque(true);
		setBorder(Gui.NONE);
		add(play);
		add(files);		
		add(cycle);		
		add(launch);		
		add(preview);		
		add(progChange);		
		add(velocity);	
		
		update();
		
		velocity.addListener(val->track.getMidiOut().setAmplification(val * 0.01f));
		progChange.addActionListener(e->{
			MidiReceiver r = track.getMidiOut();
			if (r.getProg(track.getCh()) != progChange.getSelectedIndex())
				r.progChange("" + progChange.getSelectedItem(), track.getCh());
		});
		launch.addActionListener(e->{
			state.setLaunch((int)launch.getSelectedItem());
			track.setCurrent(state.getLaunch());
		});

	}
	
	public void update() {
		play.setBackground(track.isActive() ? GREEN : null);
		setBorder(seq.getCurrent() == track ? Gui.HIGHLIGHT : Gui.NONE);
		if (cycle.getSelectedItem() != state.getCycle())
			cycle.setSelectedItem(state.getCycle());
		if (false == launch.getSelectedItem().equals(state.getLaunch()))
			launch.setSelectedItem(state.getLaunch());
		if (velocity.getValue() != track.getMidiOut().getAmplification() * 100)
			velocity.setValue((int) (track.getMidiOut().getAmplification() * 100));
		if (progChange.getSelectedIndex() != track.getMidiOut().getProg(track.getCh()))
				progChange.setSelectedIndex(track.getMidiOut().getProg(track.getCh()));
		files.update();
		preview();
		String[] patches = track.getMidiOut().getPatches();
		if (patches.length > 1) 
			if (!patches[track.getMidiOut().getProg(track.getCh())].equals(state.preset))
				track.getMidiOut().progChange(state.preset);
	}
	
	public void setState(Sched s) {
		state = s;
		track.getScheduler().setState(state);
		track.getMidiOut().setAmplification(state.amp);
		MainFrame.update(this);
	}
	
	public void preview() {
		preview.setText(track.getCurrent() + "|" + track.getNext() + "(" + track.bars() + ")");
	}
	
}
