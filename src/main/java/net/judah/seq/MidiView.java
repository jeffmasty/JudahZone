package net.judah.seq;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.midi.JudahClock;

@Getter 
public abstract class MidiView extends JPanel implements TimeListener, MidiConstants {
	
	public static enum Source { Menu, Pianist, Steps, Grid, RecPlay }

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
		if (track.isLive())
			steps.repaint();
	}
	
	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.STEP && track.isActive() && isVisible() && track.isLive()) {
			getSteps().setStart((int)value);
			getSteps().repaint();
			getGrid().repaint();
		}
		else if (prop == Property.MEASURE) {
			getSteps().timeSig();
			getGrid().timeSig();
		}
	}
	
}
