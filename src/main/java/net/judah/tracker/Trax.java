package net.judah.tracker;

import java.util.ArrayList;

import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Midi;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.midi.JudahClock;
import net.judah.util.Pastels;

public class Trax extends ArrayList<Track> implements TimeListener {

	@Getter protected final JudahClock clock;
	
	public Trax(JudahClock clock) {
		this.clock = clock;
		clock.addListener(this);
	}

	public void record(Midi midi) {
		for (Track t : this)
			if (t.isRecord()) {
				t.getCurrent().record(midi, clock.getLastPulse(), clock.getInterval()); 
				return;
			}
	}

	@Override
	public void update(Property prop, Object value) {
		if (value == JackTransportState.JackTransportStarting)
			for (Track t : this) 
				t.setStep(-1);
	}

	public void update(Track t) {
		t.getEdit().getPlayWidget().setBackground(t.isActive() ? Pastels.GREEN : Pastels.BUTTONS);
		t.getEdit().getMpk().setBackground(t.isLatch() ? Pastels.PINK : Pastels.BUTTONS);
		
		for (TrackView view : JudahZone.getTracker().getViews())
			if (view.getTrack().equals(t))
				view.update();
	}
	
	public void fileRefresh() {
			for (Track track : this) {
				if (JudahZone.getTracker() != null)
					JudahZone.getTracker().get(track).getFilename().refresh();
				track.getEdit().refreshFile();
			}
	}

}
