package net.judah.tracker;

import java.awt.GridLayout;
import java.io.Closeable;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Panic;
import net.judah.midi.Ports;
import net.judah.util.Constants;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;

@Getter
public class JudahBeatz extends JPanel implements Closeable, TimeListener {

	private final JudahClock clock;
	private final DrumTrack drum1;
	private final DrumTrack drum2;
	private final DrumTrack drum3;
	private final DrumTrack drum4;
		
	private final PianoTrack bass;
	private final PianoTrack lead1;
	private final PianoTrack lead2;
	private final PianoTrack chords;
	
	@Getter private Track[] tracks = new Track[] {};
	@Getter private final ArrayList<TrackView> views = new ArrayList<>();
	@Getter private Track current;
	@Getter private final JLabel label = new JLabel("Drum1", JLabel.CENTER);
	
	public JudahBeatz(JudahClock clock, JudahMidi midi) {
		this.clock = clock;
		clock.setTracker(this);
		clock.addListener(this);
		setLayout(new GridLayout(4, 2, 6, 6));

		Ports drums = JudahZone.getDrumPorts();
		drum1 = new DrumTrack(clock, "Drum1", drums.get(JudahZone.getBeats()), this);
		drum2 = new DrumTrack(clock, "Drum2", drums.get(JudahZone.getBeats()), this);
		drum3 = new DrumTrack(clock, "Drum3", drums.get(midi.getCalfOut()), this);
		drum4 = new DrumTrack(clock, "Drum4", drums.get(midi.getFluidOut()), this);
		
		Ports synths = JudahZone.getSynthPorts();
		bass = new PianoTrack(clock, "Bass", 1, synths.get(midi.getCraveOut()), this);
		lead1 = new PianoTrack(clock, "Lead1", 5, synths.get(JudahZone.getSynth()), this);
		lead2 = new PianoTrack(clock, "Lead2", 3, synths.get(midi.getFluidOut()), this);
		chords = new PianoTrack(clock, "Chords", 3, synths.get(JudahZone.getSynth2()), this);
		tracks = new Track[] {drum1, drum2, drum3, drum4, lead1, lead2, chords, bass};
		setCurrent(drum1);

		for(int i = 0; i < tracks.length; i+=2) {
			JPanel duo = new JPanel();
			duo.setLayout(new GridLayout(1, 2));
			Track track = tracks[i];
			track.getEdit().fillTracks();	
			TrackView view = track.getView();
			views.add(view);
			duo.add(view);
			track = tracks[i + 1];
			track.getEdit().fillTracks();	
			view = track.getView();
			views.add(view);
			duo.add(view);
			add(duo);
		}

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
	
	public int length() {
		return views.size();
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
				t.updateBorder(current);
			if (JudahZone.getFrame() != null)
				JudahZone.getBeatBox().changeTrack(track);
			if (track != null)
				label();
		}).start();
	}

	public void label() {
		if (current != null)
			label.setText(current.getName() + "/" + current.getCurrent());
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
		if (current.getView().knob(knob, data2)) {
			MainFrame.update(current);
		}

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
	
	public int indexOf(Track track) {
		for (int i = 0; i < tracks.length; i++)
			if (tracks[i] == track)
				return i;
		throw new InvalidParameterException("" + track);
	}

	@Override
	public void update(Property prop, Object value) {
		if (value == JackTransportState.JackTransportStarting)
			for (Track t : tracks) {
				t.setStep(-1);
			}
	}

	public void fileRefresh() {
		for (Track track : tracks) {
			track.getView().getFilename().refresh();
			track.getEdit().fillFile1();
        }

	}
	
	public void checkLatch() {
		for (Track t : tracks) {
			if (t.isLatch()) {
				Transpose.setActive(true);
				RTLogger.log("Transpose", "MPK -> " + t.getName());
				return;
			}
		}
		Transpose.setActive(false);
	}

	

}
