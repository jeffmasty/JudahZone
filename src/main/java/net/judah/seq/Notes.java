package net.judah.seq;

import java.util.ArrayList;

import javax.sound.midi.ShortMessage;

public class Notes extends ArrayList<MidiPair> {
	
	public boolean isBeatSelected(MidiPair note) {
		for (MidiPair p : this) 
			if (MidiTools.match(note, p))
				return true;
		return false;
	}
	
	public boolean isNoteSelected(long tick, int data1) {
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
