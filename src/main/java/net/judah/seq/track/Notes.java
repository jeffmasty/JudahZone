package net.judah.seq.track;

import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import lombok.NoArgsConstructor;

/** removing from API, move towards note-off interleaved with it's note-on */
@NoArgsConstructor
public class Notes extends ArrayList<PianoNote> {

	@Deprecated
	public Notes(Notes copy) {
		addAll(copy);
	}

	@Deprecated
	@Override public boolean contains(Object o) {
		if (o instanceof MidiEvent evt) {
			for (PianoNote note : this)
				if (note.equals(evt))
					return true;
		}
		return false;
	}

	@Deprecated
	public boolean contains(long tick, int data1) {
		for (PianoNote note : this)
			if (note.getTick() <= tick)
				if (note.getOff() == null || note.getOff().getTick() > tick) {
					if (note.getMessage() instanceof ShortMessage &&
							((ShortMessage)note.getMessage()).getData1() == data1)
					return true;
				}
		return false;
	}

}
