package net.judah.gui.knobs;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.seq.CUE;
import net.judah.seq.CYCLE;
import net.judah.seq.MidiConstants;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;
import net.judah.util.Constants;

@Getter // TODO MouseWheel listener -> change pattern 
public class TrackKnobs extends KnobPanel {

	private final MidiTrack track;
	private final Seq seq;
	private final JPanel titleBar = new JPanel();
	
	private final TrackSchedule schedule;
	private final TrackSettings settings;
	@Override
	public Component installing() {
		return titleBar;
	}
		
	//  namePlay preset |  Cycle [ABCD]    [CUE]
	//	file     vol    | (prev) [Cur]   next|after
	public TrackKnobs(MidiTrack t, Seq seq) {
		super(t.getName());
		this.track = t;
		this.seq = seq;
		JComboBox<MidiTrack> tracks = new JComboBox<>(seq.getTracks().toArray(new MidiTrack[seq.numTracks()]));
		ActionListener tracker = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				seq.getTracks().setCurrent((MidiTrack)tracks.getSelectedItem());
				tracks.removeActionListener(this);
				tracks.setSelectedItem(track);
				tracks.addActionListener(this);
			}
		};
		tracks.setSelectedItem(track);
		tracks.addActionListener(tracker);
		tracks.setFont(Gui.BOLD);
		Gui.resize(tracks, Size.COMBO_SIZE);
		titleBar.add(tracks);

		setLayout(new GridLayout(0, 1));
		settings = new TrackSettings(track);
		schedule = new TrackSchedule(track);
		add(settings);
		add(schedule);
		add(new PatternLauncher(track));
	}

	@Override
	public void update() {
		settings.update();
		schedule.update();
	}

	@Override
	public boolean doKnob(int knob, int data2) {
		switch (knob) {
			case 0: // tracknum
				int num = Constants.ratio(data2, seq.numTracks() - 1);
				seq.getTracks().setCurrent(seq.get(num));
				return true;
			case 1: // file (settable)
				File[] folder = track.getFolder().listFiles();
				File x = (File)Constants.ratio(data2, folder);
				settings.getFile().midiShow(x);
				return true;
			case 2: // pattern
				track.setFrame(Constants.ratio(data2, MidiConstants.MAX_FRAMES));
				return true;
			case 3: 
				track.setAmp(Constants.midiToFloat(data2));
				return true;
			case 4: 
				track.setCue((CUE) Constants.ratio(data2 -1, CUE.values()));
				return true;
			case 5: 
				track.setCycle((CYCLE) Constants.ratio(data2 - 1, CYCLE.values()));
				return true;
			case 6: // midiOut
				
				return true;
			case 7: 
				schedule.getProgChange().midiShow(Constants.ratio(data2 - 1, track.getMidiOut().getPatches()).toString());
				return true;
		}
		return false;		
	}

	@Override
	public void pad1() {
		MainFrame.setFocus(track);
	}

	@Override
	public void pad2() {
		
	}
	
}
