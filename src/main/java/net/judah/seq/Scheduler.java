package net.judah.seq;

import java.util.ArrayDeque;

import javax.sound.midi.MidiEvent;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.SmashHit;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahMidi;

public class Scheduler {
	
	private final MidiTrack track;
	private final Situation state;
	private final ArrayDeque<MidiEvent> realtime = new ArrayDeque<>();
	@Getter private Cycle cycle = Cycle.AB;
	@Setter private SmashHit custom;
	@Getter private int count;
	@Getter private float head;
	
	public Scheduler(MidiTrack track) {
		this.track = track;
		this.state = track.getState();
		init();
	}
	
	void init() {
		state.current = 0;
		count = 0;
		compute();
		hotSwap(0);
	}

	public void setCycle(Cycle x) {
		this.cycle = x;
		compute();
		hotSwap(head);
		MainFrame.update(track);
	}
	
	public void compute() {
		switch(cycle) {
		case A:
			state.next = state.afterNext = state.current;
			break;
		case AB: 
			if (count % 2 == 1) { 
				state.next = track.before(state.current);
				state.afterNext = state.current;
			}
			else {
				state.next = track.after((state.current));
				state.afterNext = state.current;
			}
			break;
		case A3B:
			if (count % 4 == 0 || count % 4 == 1)
				state.next = state.afterNext = state.current;
			else if (count % 4 == 2) {
				state.next = track.after(state.current);
				state.afterNext = state.current;
			}
			else if (count % 4 == 3) 
				state.next = state.afterNext = track.before(state.current);
			break;
		case ABCD:
		case ALL:
			state.next = track.after(state.current);
			state.afterNext = track.after(state.next);
		case SONG:
			break;
		}
	}
	
	void cycle() {
		if (track.isActive())
			playTo(1f);
		
		state.previous = state.current;
			count++;

		switch(cycle) {
		case A:
			state.next = state.afterNext = state.current;
			break;
		case AB:
			boolean odd = count % 2 != 0;
			if (odd) {
				state.current = track.after(state.current);
				state.next = track.before(state.current);
				state.afterNext = track.after(state.next);
			}
			else {
				state.current = track.before(state.current);
				state.next = track.after(state.current);
				state.afterNext = track.before(state.next);
			}
			break;
		case ABCD:
			boolean back = count % 4 == 0;
			if (back) {
				state.current = state.current - 4;
				if (state.current < 0)
					state.current = 0;
				state.next = track.after(state.current);
				state.afterNext = track.after(state.next);
				break;
			}
			boolean front = count % 4 <= 1;
			if (!front) {
				state.current = track.after(state.current);
				state.next = state.current - 4;
				if (state.next < 0)
					state.next = 0;
				state.afterNext = track.after(state.next);
				break;
			}
			// else, fall through to ALL:
		case ALL:
			state.current = track.after(state.current);
			state.next = track.after(state.next);
			state.afterNext = track.after(state.next);
			break;
		case A3B:
			int remain = count % 4;
			if (remain == 0) 
				state.current = state.next = state.afterNext = track.before(state.current);
			else if (remain == 1) {
				state.next = state.current;
				state.afterNext = track.after(state.current);
			}
			
			else if (remain == 2) {
				state.next = track.after(state.current);
				state.afterNext = state.current;
			}
			else if (remain == 3) {
				state.next = state.afterNext = state.current;
				state.current = track.after(state.current);
			}
			break;
		case SONG:
			break;
		}
		head = 0f;
		MainFrame.update(track);
		hotSwap();
	}

	public void hotSwap() {
		hotSwap(head);
	}
	public void hotSwap(float head) {
		MidiEvent e = realtime.poll();
		while (e != null) {
			if (e.getMessage().getStatus() == MidiTrack.NOTE_OFF)
				track.getMidiOut().send(e.getMessage(), JudahMidi.ticker());
			e = realtime.poll();
		}
		this.head = head;
		long start = (long) (track.getTicks() * head);
		Bar b = track.get(state.current);
		for (MidiEvent evt : b) {
			if (evt.getTick() < start) continue;
			realtime.add(evt);
		}
	}

	protected void playTo(float beat) {
		head = beat;
		long timecode = (long)(beat * track.getResolution());
		MidiEvent peek = realtime.peek();
		while (peek != null && peek.getTick() <= timecode) {
			track.getMidiOut().send(realtime.poll().getMessage(), JudahMidi.ticker());
			peek = realtime.peek();
		}
	}

}
