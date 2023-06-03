package net.judah.seq;

import lombok.Getter;
import net.judah.gui.settable.Cycle;
import net.judah.song.Sched;

@Getter 
public abstract class Computer {
	
	protected long barTicks;
	protected long left; // left bar's computed start tick 
	protected long right; // right bar's computed start tick
	protected Sched state;
	protected int current; // current measure/bar (not frame)
	protected int count; // increment bar cycle
	protected long recent; // sequencer sweep

	/** end of the last note */
	public abstract int bars();
	protected abstract void setCurrent(int frame);

	public boolean isActive() { return state.active; }
    public CYCLE getCycle() { return state.cycle; }
    public int getLaunch() { return state.launch; }
    public float getAmp() { return state.amp; }
	public boolean isEven() { return current % 2 == 0; }
	
	public void setCycle(CYCLE x) {
		state.cycle = x;
		count = isEven() ? 0 : 1;
		Cycle.update(this);
	}
	
	/** compute left and right frame ticks based on cycle, current bar and count */
	protected void compute() {
		switch(state.cycle) {
		case A:
			left = right = current * barTicks;
			break;
		case AB:
			if (isEven()) {
				left = current * barTicks;
				right = (current + 1) * barTicks;
			}
			else {
				left = before(current) * barTicks;
				right = current * barTicks;
			}
			break;
		case A3B:
			switch (count % 4) {
				case 0: left = right = current * barTicks; break;
				case 1: left = right = current * barTicks; break;
				case 2: left = current * barTicks; 
					right = (current + 1) * barTicks; break; 
				case 3: left = before(current) * barTicks;
					right = current * barTicks; break;
			}
			break;
		case ABCD:
		case BAR12:
			switch (count % 2) {
				case 0: 
					left = current * barTicks; 
					right = (current + 1) * barTicks; break;
				case 1: 
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
	
	protected void cycle() {
		
		int change = current;
		count++;

		// TODO what if Launch changes in realtime?
		switch(state.cycle) {
			case AB:
				change += count % 2 == 0 ? -1 /*getLaunch() - current */: 1;
				break;
			case A3B:
				switch (count % 4) {
					case 0: change = before(current); break;
					case 3: change++; break;
				}
				break;
			case ABCD:
				switch (count % 4) {
					case 0: 
						if (count != 0)
							change = before(before(before(current))); 
						break;
					case 1: 
					case 2: 
					case 3: 
						change++; 
						break;
				} 
				break;
			case BAR12: 
				change = aToX(current, 6);
				break;
			case ALL:
				change = after(current);
				break;
			case A:
				break;
		}
		if (change != current)
			setCurrent(change);
	}
	
	private int after(int idx) {
		int result = idx + 1;
		if (result >= bars()) 
			result = 0;
		return result;
	}
	private int before(int idx) {
		int result = idx - 1;
		if (result < 0)
			return bars() - 1;
		return result;
	}

	private int aToX(int input, int x) {
		if (input % x == 0) 
			return state.launch;
		return input + 1;
	}

	

}
