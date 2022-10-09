package net.judah.tracker;

import static net.judah.JudahZone.*;

import java.awt.GridLayout;
import java.io.Closeable;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.midi.Panic;
import net.judah.util.Constants;

public class Tracker extends JPanel implements Closeable {

	public static final String NAME = "Tracker";
	
	@Getter private Track current;
	@Getter private final JLabel feedback = new JLabel("Drum1", JLabel.CENTER);
	@Getter private final ArrayList<TrackView> views = new ArrayList<>();
	@Getter private final int length;
	
	public Tracker(JudahBeatz drumTracks, JudahNotez pianoTracks) {
		length = drumTracks.size() + pianoTracks.size();
		// setLayout(new GridLayout(4, 2, 6, 6));
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		for(int i = 0; i < drumTracks.size(); i+=2) 
			createTrackDuo(drumTracks.get(i), 
					drumTracks.size() > i+1 ? drumTracks.get(i+1) : null);
			
		for (int i = 0; i < pianoTracks.size(); i+=2)
			createTrackDuo(pianoTracks.get(i), 
					pianoTracks.size() > i+1 ? pianoTracks.get(i+1) : null);
		add(Box.createVerticalGlue());
		setCurrent(drumTracks.get(3));
		JudahZone.getServices().add(this);
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
		for (TrackView v : views)
			if (v.getTrack() == current)
				v.knob(knob, data2);
	}
	
	@Override
	public void close() {
		for (TrackView view : views) {
			Track t = view.getTrack();
			if (t.isActive() && t.isSynth())
				new Panic(t.getMidiOut(), t.getCh()).start();
		}
	}

	
	private void createTrackDuo(Track one, Track two) {
		JPanel duo = new JPanel();
		duo.setOpaque(true);
		duo.setLayout(new GridLayout(1, 2));
		one.getEdit().fillTracks(length);	
		TrackView view = new TrackView(one, this);
		views.add(view);
		duo.add(view);
		if (two == null) 
			duo.add(new JLabel(" "));
		else {
			two.getEdit().fillTracks(length);	
			view = new TrackView(two, this);
			views.add(view);
			duo.add(view);
		}
		add(duo);
	}

	public boolean isRecord() {
		for (Track t : getBeats())
			if (t.isRecord())
				return true;
		for (Track t : getNotes())
			if (t.isRecord())
				return true;

		return false;
	}

	
	
	public void feedback() {
		if (current != null)
			feedback.setText(current.getName() + "/" + current.getCurrent());
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
			if (getFrame() != null)
				getBeatBox().changeTrack(track);
			if (track != null)
				feedback();
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

	public void update(Track t) {
		t.getEdit().update();
		for (TrackView v : views)
			if (v.getTrack() == t)
				v.update();
	}

	public Track get(int idx) {
		return views.get(idx).getTrack();
	}

	public void setCurrent(int channel) {
		setCurrent(views.get(channel).getTrack());
	}

	public TrackView get(Track track) {
		for (TrackView t : views)
			if (t.getTrack() == track)
				return t;
		return null;
	}
	
	private static final ArrayList<Track> _all = new ArrayList<>();
	/** dynamic from Tracker instances */
	public static ArrayList<Track> getAll() {
		_all.clear();
		_all.addAll(JudahZone.getBeats());
		_all.addAll(JudahZone.getNotes());
		return _all;
	}

}
