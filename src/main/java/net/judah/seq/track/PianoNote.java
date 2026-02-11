package net.judah.seq.track;

import javax.sound.midi.MidiEvent;

import judahzone.api.Midi;
import lombok.Getter;
import lombok.Setter;

/** Use only in special circumstances, normal method is to interleave note-off midievents with their note-ons */
public class PianoNote extends MidiEvent {

	@Getter @Setter private MidiEvent off;

	public PianoNote(MidiEvent on) {
		super(on.getMessage(), on.getTick());
	}

	public PianoNote(MidiEvent on, MidiEvent off) {
		this(on);
		this.off = off;
	}

	@Override public int hashCode() {
		return super.hashCode() + (off == null ? 0 : off.hashCode());
	}

	@Override public boolean equals(Object o) {

		if (o instanceof MidiEvent evt)
			return getMessage().equals(evt.getMessage()) && getTick() == evt.getTick();
		return false;
	}

	@Override public String toString() {
		return "on:" + Midi.toString(getMessage()) + "@" + getTick() + " off:" + (off == null ? "null" :
			Midi.toString(off.getMessage()) + "@" + off.getTick());
	}


}
