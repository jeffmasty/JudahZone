package net.judah.seq;

import java.util.ArrayList;

import javax.sound.midi.ShortMessage;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Notes extends ArrayList<MidiPair> {
	
	public Notes(Notes copy) {
		addAll(copy);
	}
	
	
	@Override public boolean contains(Object o) {
		if (o instanceof MidiPair == false) return false;
		MidiPair note = (MidiPair)o;
		for (MidiPair p : this) 
			if (MidiTools.match(note, p))
				return true;
		return false;
	}
	
	public boolean contains(long tick, int data1) {
		for (MidiPair p : this)
			if (p.getOn().getTick() <= tick)
				if (p.getOff() == null || p.getOff().getTick() > tick) {
					if (p.getOn().getMessage() instanceof ShortMessage && 
							((ShortMessage)p.getOn().getMessage()).getData1() == data1)
					return true;
				}
		return false;
	}


}
