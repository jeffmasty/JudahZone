package net.judah.seq;

import lombok.Getter;
import net.judah.api.MidiReceiver;
import net.judah.gui.MainFrame;
import net.judah.midi.Panic;
import net.judah.song.Sched;

@Getter 
public class Scheduler {
	
	// current/serialize state
	private Sched state = new Sched();
	
	
	
	private int current;
	Cue cue = Cue.Bar; // @JsonIgnore?
	private final MidiTrack track;
	int previous;
	int next;
	int afterNext;
	private int count;
	
	public Scheduler(MidiTrack track) {
		this.track = track;
		init();
	}
	
	void init() {
		current = 0;
		count = 0;
		compute();
	}

	public void setCount(int count) {
		this.count = count;
		compute();
		MainFrame.update(track);
	}
	
	public Cycle getCycle() {
		return state.cycle;
	}
	
	public void setCycle(Cycle x) {
		state.setCycle(x);
		compute();
		track.flush(1f);
		MainFrame.update(track);
	}
	
	public void compute() {
		switch(state.cycle) {
		case A:
			next = afterNext = current;
			break;
		case AB: 
			if (count % 2 == 1) { 
				next = before(current);
				afterNext = current;
			}
			else {
				next = after((current));
				afterNext = current;
			}
			break;
		case A3B:
			if (count % 4 == 0 || count % 4 == 1)
				next = afterNext = current;
			else if (count % 4 == 2) {
				next = after(current);
				afterNext = current;
			}
			else if (count % 4 == 3) 
				next = afterNext = before(current);
			break;
		case ABCD:
		case ALL:
			next = after(current);
			afterNext = after(next);
		case SONG:
			break;
		}
		track.oldTime = current * track.getBarTicks() - 1;
	}
	
	void cycle() {
		if (track.isActive())
			track.flush(1f);
			
		previous = current;
		count++;

		switch(state.cycle) {
		case A:
			next = afterNext = current;
			break;
		case AB:
			boolean odd = count % 2 != 0;
			if (odd) {
				current = after(current);
				next = before(current);
				afterNext = after(next);
			}
			else {
				current = before(current);
				next = after(current);
				afterNext = before(next);
			}
			break;
		case ABCD:
			boolean back = count % 4 == 0;
			if (back) {
				current = current - 4;
				if (current < 0)
					current = 0;
				next = after(current);
				afterNext = after(next);
				break;
			}
			boolean front = count % 4 <= 1;
			if (!front) {
				current = after(current);
				next = current - 4;
				if (next < 0)
					next = 0;
				afterNext = after(next);
				break;
			}
			// else, fall through to ALL:
		case ALL:
			current = after(current);
			next = after(next);
			afterNext = after(next);
			break;
		case A3B:
			int remain = count % 4;
			if (remain == 0) 
				current = next = afterNext = before(current);
			else if (remain == 1) {
				next = current;
				afterNext = after(current);
			}
			
			else if (remain == 2) {
				next = after(current);
				afterNext = current;
			}
			else if (remain == 3) {
				next = afterNext = current;
				current = after(current);
			}
			break;
		case SONG:
			break;
		}
		track.head = 0f;
		track.oldTime = current * track.getBarTicks() - 1;

		MainFrame.update(track);
		
	}

	private int after(int idx) {
		int result = idx + 1;
		if (result >= track.bars())
			return 0;
		return result;
	}
	private int before(int idx) {
		int result = idx - 1;
		if (result < 0)
			return track.bars() - 1;
		return result;
	}

	public void setCurrent(int change) {
    	previous = current;
    	current = change;
    	MainFrame.update(track);
    	compute();
	}

	public boolean isActive() {
		return state.active;
	}

	public void setActive(boolean active) {
		state.active = active;
		if (!active)
			new Panic(track.getMidiOut(), track.getCh()).start();
		MainFrame.update(track);

	}

	public Sched getState() {
		state.setAmp(track.getMidiOut().getAmplification());
		MidiReceiver midi = track.getMidiOut();
		state.setPreset(midi.getPatches()[midi.getProg(track.getCh())]);
		// state.setLaunch(current);
		return state;
	}
	
	public void setState(Sched sched) {
		int old = current; 
		state = sched;
		if (state.launch != old)
		setCurrent(state.launch);
	}
	
}
