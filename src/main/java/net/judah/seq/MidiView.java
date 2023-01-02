package net.judah.seq;

import javax.swing.JPanel;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.midi.JudahClock;

@Getter 
public abstract class MidiView extends JPanel implements TimeListener, MidiConstants {
	
	public static enum Source { Menu, Pianist, Steps, Grid, RecPlay }

	protected final JudahClock clock;
	protected final MidiTrack track;
	protected MidiMenu menu;
	protected MusicGrid grid;
	protected JPanel instrumentPanel;
	
	protected final Measure scroll;
	protected final Notes selected = new Notes();
	@Setter protected long timeframe; // 2 bars
	@Setter protected boolean live;
	
	public MidiView(MidiTrack t) {
		this.track = t;
		this.clock = track.getClock();
		scroll = new Measure(track);
		timeframe = clock.getMeasure() * track.getResolution() * 2;
		setLayout(null);
	}
	
	public abstract Steps getSteps();
	public abstract void update();
	
	public static float ratioY(long tick, long measure) {
		return tick / (float)measure;
	}

	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.STEP && track.isActive() && isVisible() && isLive()) {
			getSteps().setStart((int)value);
			getSteps().repaint();
			getGrid().repaint();
		}
		else if (prop == Property.MEASURE) {
			setTimeframe(2 * track.getResolution() * (int)value); // untested
		}
	}
	
}
