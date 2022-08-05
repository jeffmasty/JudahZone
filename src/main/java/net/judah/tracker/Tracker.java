package net.judah.tracker;

import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPK;
import net.judah.util.Constants;

public class Tracker extends JPanel {

	@Getter private final ArrayList<TrackView> views = new ArrayList<>();
	@Getter static private Track current;
	
	public Tracker(Track[] tracks) {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		for(Track track : tracks) {
			track.getGrid().fillTracks();	
			TrackView view = track.getView();
			views.add(view);
			add(view);
			
		}
		
		current = tracks[0];
	}

	public void update(Track t) {
		for (TrackView view : views)
			if (view.getTrack().equals(t))
				view.update();
	}

	public void setCurrent(Track track) {
		if (current == track) return;
		for (TrackView t : views)
			//set old current to standard border
			if (t.getTrack() == current)
				t.setBorder(Constants.Gui.NONE);
		if (track == null)
			return;
		current = track;	
		for (TrackView t : views)
			if (t.getTrack() == current)
				t.setBorder(Constants.Gui.HIGHLIGHT);
		MPK.setMode(KnobMode.Tracks);
		// MainFrame.updateCurrent();
		MainFrame.get().getBeatBox().changeTrack(track);
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
	
}
