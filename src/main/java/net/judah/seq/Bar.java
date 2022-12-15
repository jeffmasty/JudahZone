package net.judah.seq;

import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.judah.midi.Midi;

/** List of Midi events ordered by tick */
@Getter @AllArgsConstructor
public class Bar extends ArrayList<MidiEvent> {

//	@Setter private String name;
	
//	private int idx;
	
	public Bar(Bar bar) {
		for (MidiEvent e : bar)
			add(new MidiEvent(new Midi(e.getMessage().getMessage()), e.getTick()));
	}

	@Override
	public boolean add(MidiEvent e) {
		long on = e.getTick();
		for (int i = 0; i < size(); i++) 
			if (get(i).getTick() > on) {
				super.add(i, e);
				return true;
			}
		return super.add(e);
	}

	public MidiEvent reverseSearch(int cmd, long fromTime, int data1) {
		for (int i = size() - 1; i >= 0; i--) {
			MidiEvent e = get(i);
			if (e.getTick() > fromTime)
				continue;
			ShortMessage m = (ShortMessage)e.getMessage();
			if (m.getCommand() != cmd)
				continue;
			if (m.getData1() == data1)
				return e;
		}
		return null;
	}

	public MidiEvent search(int cmd, long fromTime, int data1) {
		for (MidiEvent e : this) {
			if (e.getTick() < fromTime)
				continue;
			ShortMessage m = (ShortMessage)e.getMessage();
			if (m.getCommand() != cmd)
				continue;
			if (m.getData1() == data1)
				return e;
		}
		return null;
	}
	@Override
	public String toString() {
		return "ERROR";
	}
	
	
//	public List<ShortMessage> seek(long timecode) {
//		return null;
//	}

}
