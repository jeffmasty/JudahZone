package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Track;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.song.Sched;

@Getter
public class Computer {

	public static enum Update {
		PLAY, CAPTURE, CURRENT, CYCLE, AMP, ARP, PROGRAM,
		CUE, GATE, FILE, REFILL, RANGE, LAUNCH, REZ
	}
//	public static record Tracker(Update type, Computer track) {}

	protected JudahClock clock = JudahMidi.getClock();

	/** Sequence (for save) */
    protected final Track t;
    protected int resolution = MidiFile.DEFAULT_RESOLUTION;
	protected long recent; // sequencer sweep

	@Setter protected long barTicks; // ticks in resolution of timeSig
	protected Sched state = new Sched(); // assigned when Song Scene changes
	protected int current; 	// current measure/bar (not frame)
	protected int offset; 	// frame adjustment by user away from Scene.launch
	protected int count; 	// internal cycle bar increment
	protected long left; 	// left bar's computed start tick
	protected long right; 	// right bar's computed start tick


	public Computer() throws InvalidMidiDataException {
		t = new MidiFile().createTrack();
		setBarTicks(clock.getTimeSig().beats * resolution);
		compute();
		//setResolution(MidiFile.DEFAULT_RESOLUTION);
	}

	/** end of the last note */
	public final int getFrames() { return (int) Math.ceil(bars() / 2f); }
	public final boolean isActive() { return state.active; }
    public Cycle getCycle() { return state.cycle; }
    public int getLaunch() { return state.launch; }
    public float getAmp() { return state.amp; }
	public int getFrame() {return current / 2;}
    public long getWindow() { return 2 * barTicks; }
	public long getStepTicks() { return resolution / clock.getSubdivision(); }
	/**@return number of bars with notes recorded into them */
	public final int bars() { return MidiTools.measureCount(t.ticks(), barTicks); }

    public void setResolution(int rez) { // TODO
		if (rez < 2 || rez > 2048)
			throw new NumberFormatException("out of bounds");
		float factor = rez / (float)getResolution();
		for (int i = t.size() - 1; i >= 0; i--) {
			t.get(i).setTick((long) (t.get(i).getTick() * factor));
		}
		resolution = rez;
		setBarTicks(clock.getTimeSig().beats * resolution);
		compute();
		MainFrame.updateTrack(Update.REZ, this);
    }

	protected void setCurrent(int change) {
		if (current == change) return;
		if (change < 0)
			change = clock.isEven() ? 0 : 1;
		recent = change * barTicks + (recent - current * barTicks);
		current = change;
		compute();
		MainFrame.updateTrack(Update.CURRENT, this);
	}

	public void setCycle(Cycle x) {
		state.cycle = x;
		count = clock.isEven() ? 0 : 1;
		compute();
		MainFrame.updateTrack(Update.CYCLE, this);
	}

	protected void reset() {
		count = clock.isEven() ? 0 : 1;
		setCurrent(state.launch + 2 * offset + count);
	}

	/** sequence to next bar */
	protected void cycle() {
		count++;
		int change = -1;
		switch(state.cycle) {
			case AB:
				if (clock.isEven()) reset();
				else change = current + 1;
				break;
			case A3B:
				switch (count % 4) {
					case 0: reset(); break;
					case 3: change = current + 1; break;
				}
				break;
			case ABCD:
				switch (count % 4) {
					case 0: reset();
						break;
					case 1:
					case 2:
					case 3:
						change = current + 1;
						break;
				}
				break;
			case ALL:
				change = after(current);
				break;
			case A:
				break;
		case TO8:
		case TO12:
		case TO16:
			if (count >= state.cycle.getLength())
				reset();
			else
				change = after(current);
			break;
		case CLCK:
			if (count >= JudahClock.getLength())
				reset();
			else
				change = after(current);
			break;
		}
		if (change >= 0 && change != current)
			setCurrent(change);
	}

	/** compute left and right frame ticks based current state */
	protected void compute() {
		switch(state.cycle) {
		case A:
			left = right = current * barTicks;
			break;
		case AB:	case ABCD:	case TO8:
		case TO12:	case TO16: case CLCK:
			left = clock.isEven() ? current * barTicks : before(current) * barTicks;
			right = clock.isEven() ? (current + 1) * barTicks : current * barTicks;
			break;
		case A3B:
			switch (count % 4) {
				case 0: case 1:
					left = right = current * barTicks; break;
				case 2:
					left = current * barTicks;
					right = (current + 1) * barTicks; break;
				case 3:
					left = before(current) * barTicks;
					right = current * barTicks; break;
			}
			break;
		case ALL:
			left = clock.isEven() ? current * barTicks : before(current) * barTicks;
			right = clock.isEven() ? after(current) * barTicks : current * barTicks;
			break;
		}
	}

	int after(int idx) {
		int result = idx + 1;
		if (result >= bars())
			result = 0;
		return result;
	}
	int before(int idx) {
		int result = idx - 1;
		if (result < 0)
			return bars() - 1;
		return result;
	}

	public void next(boolean fwd) {
		offset += fwd ? 1 : -1;
		int next = fwd ? after(after(current)) : before(before(current));
		if (next % 2 == 0 != clock.isEven())
			next++;
		setCurrent(next);
	}

	public void setLaunch(int bar) {
		state.launch = bar;
		offset = 0;
		MainFrame.updateTrack(Update.LAUNCH, this);
	}

	public void toFrame(int frame) {
		if (current / 2 == frame)
			return;
		offset = frame - count / 2 - state.launch / 2;
		if (offset >= getFrames())
			offset -= getFrames();
		if (offset <= getFrames() * -1)
			offset += getFrames();
		setCurrent(frame * 2);
	}

}