package net.judah.seq;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.api.Notification.Property;
import net.judah.api.Signature;
import net.judah.api.TimeListener;
import net.judah.midi.JudahClock;
import net.judah.seq.piano.Piano;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.TrackMenu;

@Getter
public abstract class MidiView extends JPanel implements TimeListener, MidiConstants {

	public static enum Source {
		Menu, Pianist, Steps, Grid, RecPlay
	}

	protected final JudahClock clock;
	protected final MidiTrack track;
	protected TrackMenu menu;
	protected Piano grid;

	public MidiView(MidiTrack t) {
		setName(t.getName());
		this.track = t;
		this.clock = track.getClock();
		clock.addListener(this);
		setLayout(null);
	}

	public void update() {
		menu.update();
		grid.repaint();
	}

	@Override
	public void update(Property prop, Object value) {
		// if (prop == Property.STEP && track.isActive() && isVisible()) {
		// getSteps().setStart((int)value); // waterfall
		// getGrid().repaint();
		// } else
		if (value instanceof Signature sig) {
			getGrid().timeSig(sig);
//			if (this instanceof PianoView piano)
//				piano.getSteps().timeSig(sig);
		}
	}

}
