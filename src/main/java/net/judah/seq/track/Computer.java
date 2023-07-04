package net.judah.seq.track;

import static net.judah.midi.JudahClock.isEven;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.CycleCombo;
import net.judah.midi.JudahClock;
import net.judah.song.Sched;

@Getter 
public abstract class Computer {
	
	@Setter protected long barTicks; // ticks in resolution of timeSig
	protected Sched state = new Sched(); // from Song file Scenes
	protected int current; 	// current measure/bar (not frame)
	protected int offset; 	// frame adjustment away from Scene.launch by user
	protected int count; 	// internal cycle bar increment
	protected long left; 	// left bar's computed start tick 
	protected long right; 	// right bar's computed start tick

	/** end of the last note */
	public abstract int bars();
	public final int frames() { return (int) Math.ceil(bars() / 2f); }
	public boolean isActive() { return state.active; }
    public Cycle getCycle() { return state.cycle; }
    public int getLaunch() { return state.launch; }
    public float getAmp() { return state.amp; }
	public int getFrame() {return current / 2;}
	protected abstract void setCurrent(int bar);
	protected abstract void flush();

	public void setCycle(Cycle x) { 
		state.cycle = x;
		count = isEven() ? 0 : 1;
		compute();
		CycleCombo.update(this);
		MainFrame.update(this);
	}
	
	protected void reset() {
		count = isEven() ? 0 : 1;
		flush();
		setCurrent(state.launch + 2 * offset + count);
	}
	
	/** sequence to next bar */
	protected void cycle() {
		flush();
		count++;
		int change = -1;
		switch(state.cycle) {
			case AB:
				if (isEven()) reset();
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
		case AB:	case ABCD:	case TO12:
		case TO8:	case CLCK:
			left = isEven() ? current * barTicks : before(current) * barTicks;
			right = isEven() ? (current + 1) * barTicks : current * barTicks; 
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
			left = isEven() ? current * barTicks : before(current) * barTicks;
			right = isEven() ? after(current) * barTicks : current * barTicks;
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
		flush();
		offset += fwd ? 1 : -1;
		int next = fwd ? after(after(current)) : before(before(current));
		if (next % 2 == 0 != JudahClock.isEven())
			next++;
		setCurrent(next);
	}

	public void reLaunch() {
		setLaunch(state.launch + 2 * offset);
	}
	
	private void setLaunch(int bar) {
		state.launch = bar;
		offset = 0;
		MainFrame.update(this);
	}
	
	public void toFrame(int frame) {
		if (current / 2 == frame)
			return;
		flush();
		offset = frame - count / 2 - state.launch / 2;
		if (offset >= frames())
			offset -= frames();
		if (offset <= frames() * -1)
			offset += frames();
		setCurrent(frame * 2);
	}	

}