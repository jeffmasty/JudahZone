package net.judah.seq;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.midi.JudahClock;
import net.judah.midi.Signature;

@Getter
public abstract class MidiView extends JPanel implements TimeListener, MidiConstants {

	public static enum Source {
		Menu, Pianist, Steps, Grid, RecPlay
	}

	protected final JudahClock clock;
	protected final MidiTrack track;
	protected MidiMenu menu;
	protected MusicBox grid;
	protected JPanel instrumentPanel;
	protected Steps steps;

	public MidiView(MidiTrack t) {
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
		if (prop == Property.SIGNATURE) {
			getSteps().timeSig((Signature) value);
			getGrid().timeSig((Signature) value);
		}
	}

}
