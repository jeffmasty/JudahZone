package net.judah.song;

import static net.judah.gui.Pastels.GREEN;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.settable.Cycle;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Arrow;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.TrackVol;
import net.judah.seq.MidiConstants;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;

@Getter // TODO MouseWheel listener -> change pattern? 
public class SongTrack extends JPanel implements Size {
	
	private final MidiTrack track;
	private final Seq seq;
	private Sched state = new Sched();
	private final JButton play;
	private final TrackVol vol;
	private final Cycle cycle;
	private final JComboBox<Integer> launch = new JComboBox<>();
	private final Program progChange;
	private final Folder files;
	private final Btn clear;
	private final JLabel total = new JLabel();
	private final ActionListener fileListener = new ActionListener() {
			@Override public void actionPerformed(ActionEvent evt) {
				track.load(new File(track.getFolder(), files.getSelectedItem().toString()));}};
	private final JButton edit = new JButton(UIManager.getIcon("FileChooser.detailsViewIcon"));
				
	//  namePlay file cycle bar preview preset amp
	public SongTrack(MidiTrack t, Seq seq) {
		this.track = t;
		this.seq = seq;
		play = new Btn("▶️ " + t.getName(), e-> {
			if (track.isActive() || track.isOnDeck()) 
				track.setActive(false);
			else track.setActive(true);});
		play.setOpaque(true);
		edit.addActionListener(e->JudahZone.getFrame().edit(track));
		progChange = new Program(track.getMidiOut(), track.getCh()); // no midi controller connection, but participates in updates
		files = new Folder(track);
		cycle = new Cycle(track);
		vol = new TrackVol(track);
		clear = new Btn("x", e->track.clear());
		Gui.resize(play, SMALLER_COMBO);
		Gui.resize(progChange, COMBO_SIZE);
		Gui.resize(files, COMBO_SIZE);
		
		// bars
		for (int i = 0; i <= MidiConstants.MAX_FRAMES; i++)
			launch.addItem(i);
		launch.setSelectedIndex(0);
		
		setOpaque(true);
		setBorder(Gui.SUBTLE);
		add(play);
		add(files);		
		add(clear);
		add(progChange);		
		add(cycle);		
		
		add(new Arrow(Arrow.WEST, e->next(false)));
		add(launch);		
		add(total);		
		add(new Arrow(Arrow.EAST, e->next(true)));
		add(edit);
		add(vol);	
		
		update();
		
		launch.addActionListener(e->{
			if (state.getLaunch() == (int)launch.getSelectedItem())
				return;
			launch((int)launch.getSelectedItem());
		});
	}
	
	private void launch(int i) {
		state.setLaunch(i);
		track.setFrame(i);
		MainFrame.update(this);
	}
	
	private void next(boolean fwd) {
		int next = launch.getSelectedIndex() + (fwd ? 1 : -1);
		if (next < 0)
			next = 0;
		if (next >= launch.getItemCount())
			next = 0;
		launch(next);
	}
	
	public void update() {
		play.setBackground(track.isActive() ? GREEN : null);
		if (false == launch.getSelectedItem().equals(state.getLaunch()))
			launch.setSelectedItem(state.getLaunch());
		if (vol.getValue() != track.getAmplification())
			vol.setValue((track.getAmplification()));
		files.update();
		total.setText("" + track.frames());
	}
	
	public void setState(Sched s) {
		state = s;
		track.setState(state);
		MainFrame.update(this);
	}
	
}
