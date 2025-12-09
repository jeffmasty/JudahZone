package net.judah.seq;

import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Notes extends ArrayList<MidiNote> {

	public Notes(Notes copy) {
		addAll(copy);
	}

	@Override public boolean contains(Object o) {
		if (o instanceof MidiEvent evt) {
			for (MidiNote note : this)
				if (note.equals(evt))
					return true;
		}
		return false;
//		if (o instanceof MidiPair note)
//			for (MidiPair p : this)
//				if (MidiTools.match(note, p))
//					return true;
//		return false;
	}

	public boolean contains(long tick, int data1) {
		for (MidiNote note : this)
			if (note.getTick() <= tick)
				if (note.getOff() == null || note.getOff().getTick() > tick) {
					if (note.getMessage() instanceof ShortMessage &&
							((ShortMessage)note.getMessage()).getData1() == data1)
					return true;
				}
		return false;
	}

//	public ArrayList<MidiEvent> list() {
//		ArrayList<MidiEvent> result = new ArrayList<MidiEvent>();
//		for (MidiNote note : this) {
//			result.add(p.getOn());
//			if (p.getOff() != null)
//				result.add(p.getOff());
//		}
//		return result;
//	}


}
