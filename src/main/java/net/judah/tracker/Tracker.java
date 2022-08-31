package net.judah.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.api.Service;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPK;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Panic;
import net.judah.util.Constants;
import net.judah.util.Pastels;

@Getter
public class Tracker extends JPanel implements Service {

	private final JudahClock clock;
	private final DrumTrack drum1;
	private final DrumTrack drum2;
	private final DrumTrack drum3;
		
	private final PianoTrack bass;
	private final PianoTrack lead1;
	private final PianoTrack lead2;
	private final PianoTrack chords;
	
	private final Track[] tracks;
	private final ArrayList<TrackView> views = new ArrayList<>();
	@Getter private static Track current;
	
	public Tracker(JudahClock clock, JudahMidi midi) {
		this.clock = clock;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
	
		drum1 = new DrumTrack(clock, "Drum1", midi.getCalfOut());
		drum2 = new DrumTrack(clock, "Drum2", midi.getCalfOut());
		drum3 = new DrumTrack(clock, "Drum3", midi.getFluidOut());
		
		bass = new PianoTrack(clock, "Bass", 1, midi.getCraveOut());
		lead1 = new PianoTrack(clock, "Lead1", 5, midi.getFluidOut());
		lead2 = new PianoTrack(clock, "Lead2", 3, midi.getCircuitOut());
		chords = new PianoTrack(clock, "Chords", 3, midi.getFluidOut());
		tracks = new Track[] {drum1, drum2, drum3, bass, lead1, lead2, chords};
		for(Track track : tracks) {
			track.getEdit().fillTracks();	
			TrackView view = track.getView();
			views.add(view);
			add(view);
		}
		
		current = tracks[0];
		JudahZone.getServices().add(0, this);
	}


	public void update(Track t) {
		t.getEdit().getPlayWidget().setBackground(t.isActive() ? Pastels.GREEN : Pastels.BUTTONS);
		t.getEdit().getMpk().setBackground(t.isLatch() ? Pastels.PINK : Pastels.BUTTONS);
		
		
		
		for (TrackView view : views)
			if (view.getTrack().equals(t))
				view.update();
	}

	@Override
	public void close() {
		for (TrackView view : views) {
			Track t = view.getTrack();
			if (t.isActive() && t.isSynth())
				new Panic(t.getMidiOut(), t.getCh()).start();
		}
	}
	
	public void setCurrent(Track track) {
		if (current == track) return;
		new Thread(() -> {
			for (TrackView t : views)
				//set old current to standard border
				if (t.getTrack() == current)
					t.setBorder(Constants.Gui.NONE);
			if (track == null) {
				current.getEdit().setBorder(Constants.Gui.NO_BORDERS);
				return;
			}
			current = track;	
			current.getEdit().setBorder(Constants.Gui.HIGHLIGHT);
			for (TrackView t : views)
				if (t.getTrack() == current)
					t.setBorder(Constants.Gui.HIGHLIGHT);
			MPK.setMode(KnobMode.Tracks);
			// MainFrame.updateCurrent();
			MainFrame.get().getBeatBox().changeTrack(track);
		}).start();
	}

	public void changeTrack(boolean up) {
		if (current == null) current = views.get(0).getTrack();
		if (up == true) {
			if (current == views.get(0).getTrack()) {
				setCurrent(views.get(views.size() - 1).getTrack());
				return;
			}
			for (int i = 1; i < views.size(); i++) 
				if (current == views.get(i).getTrack()) {
					setCurrent(views.get(i - 1).getTrack());
					return;
				} 
		} else {
			if (current == views.get(views.size() - 1).getTrack()) {
				setCurrent(views.get(0).getTrack());
				return;
			}
			for (int i = 0; i < views.size() - 1; i++)
				if (current == views.get(i).getTrack()) {
					setCurrent(views.get(i + 1).getTrack());
					return;
				}
		}
	}

	public TrackView getView(Track t) {
		for (TrackView v : views)
			if (v.getTrack() == t)
				return v;
		return null;
	}
	
	public void changePattern(boolean up) {
		if (current == null) current = views.get(0).getTrack();
		current.next(up);
		MainFrame.update(current);
	}

	public void knob(int knob, int data2) {
		if (current.getView().process(knob, data2)) {
			MainFrame.update(current);
		}

	}

	@Override
	public List<Command> getCommands() {
		return Collections.emptyList();
	}

	@Override
	public void properties(HashMap<String, Object> props) {
	}
	
	public boolean isRecord() {
		for (Track t : tracks)
			if (t.isRecord())
				return true;
		return false;
	}
	
	public void record(Midi midi) {
		for (Track t : tracks)
			if (t.isRecord()) {
				t.getCurrent().record(midi, clock.getLastPulse(), clock.getInterval()); 
				return;
			}
	}
	
}
