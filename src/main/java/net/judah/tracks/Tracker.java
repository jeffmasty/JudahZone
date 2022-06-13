package net.judah.tracks;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPK;
import net.judah.midi.Panic;
import net.judah.util.Constants;

public class Tracker extends JPanel {

	@Getter private final ArrayList<TrackView> tracks = new ArrayList<>();
	private Track focus;
	
	public Tracker(Track[] tracks) {
		setLayout(new GridLayout(8, 2));
		for(Track track : tracks) {
			TrackView view = TrackView.create(track);
			this.tracks.add(view);
			add(view);
			add(track.getFeedback());
		}
		focus = tracks[0];
			
	}

	public void update(Track t) {
		for (TrackView view : tracks)
			if (view.getTrack().equals(t))
				view.update();
	}

	public void setFocus(Track track) {
		if (focus == track) return;
		for (TrackView t : tracks)
			//set old focus to standard border
			if (t.getTrack() == focus)
				t.setBorder(Constants.Gui.NO_BORDERS);
		focus = track;	
		for (TrackView t : tracks)
			if (t.getTrack() == focus)
				t.setBorder(Constants.Gui.FOCUS_BORDER);
		MPK.setMode(KnobMode.Tracks);
		MainFrame.updateCurrent();
	}

	public void changeTrack(boolean up) {
		if (focus == null) focus = tracks.get(0).getTrack();
		if (up == true) {
			if (focus == tracks.get(0).getTrack()) {
				setFocus(tracks.get(tracks.size() - 1).getTrack());
				return;
			}
			for (int i = 1; i < tracks.size(); i++) 
				if (focus == tracks.get(i).getTrack()) {
					setFocus(tracks.get(i - 1).getTrack());
					return;
				} 
		} else {
			if (focus == tracks.get(tracks.size() - 1).getTrack()) {
				setFocus(tracks.get(0).getTrack());
				return;
			}
			for (int i = 0; i < tracks.size() - 1; i++)
				if (focus == tracks.get(i).getTrack()) {
					setFocus(tracks.get(i + 1).getTrack());
					return;
				}
		}
	}

	public TrackView getView(Track t) {
		for (TrackView v : tracks)
			if (v.getTrack() == t)
				return v;
		return null;
	}
	
	public void changePattern(boolean up) {
		if (focus == null) focus = tracks.get(0).getTrack();
		if (focus instanceof StepTrack) 
			((StepTrack)focus).changePattern(up);
		else if (focus instanceof KitTrack) {
			Box box = ((KitTrack)focus).getBeatbox();
			Box.next(true, box, box.getCurrent());
		}
		MainFrame.update(focus);
	}

	public void knob(int knob, int data2) {
		if (focus.process(knob, data2)) { // allow subclass handling
			MainFrame.update(focus);
			return;
		}

		if (knob == 0) { // file
			focus.selectFile(data2);
		}
		else if (knob == 1) { // midiOut
			TrackView view = getView(focus);
			ArrayList<JackPort> available = view.getMidiOut().getPorts();
			Object obj = Constants.ratio(data2, available);
			JackPort old = focus.getMidiOut();
			if (focus.setMidiOut((JackPort)obj)) {
				view.redoInstruments();
				if (old != null) 
					new Panic(old).start();
			}
				
			MainFrame.update(focus);
		}
		else if (knob == 2) { // instrument
			if (focus instanceof StepTrack)
				((StepTrack)focus).setInstrument(0, data2);
			else {
				TrackView view = getView(focus);
				int idx = 1 + Constants.ratio(data2, view.getInstruments().getItemCount() - 2);
				Instrument.set(focus, idx);
				view.selectInstrument(idx);
			}
		}
		else if (knob == 3) {
			focus.setGain(data2 * 0.01f);
			MainFrame.update(focus);
		}
	}

	public void loadMidi() {
		// file box
		// setFile on Track 7
		
	}
	
}
