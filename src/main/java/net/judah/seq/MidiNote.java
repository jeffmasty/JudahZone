package net.judah.seq;

import javax.sound.midi.MidiEvent;

import lombok.Getter;
import lombok.Setter;
import net.judah.midi.Midi;

public class MidiNote extends MidiEvent {

	@Getter @Setter private MidiEvent off;

	public MidiNote(MidiEvent on) {
		super(on.getMessage(), on.getTick());
	}

	public MidiNote(MidiEvent on, MidiEvent off) {
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

//		if (false == o instanceof MidiEvent) return false;
//    	MidiEvent it = (MidiEvent)o;
//    	if (off == null)
//    		if (it.off != null) return false;
//    		else return on.equals(it.on);
//    	else if (it.off == null) return false;
//    	else return on.equals(it.on) && off.equals(it.off);

	@Override public String toString() {
		return "on:" + Midi.toString(getMessage()) + "@" + getTick() + " off:" + (off == null ? "null" :
			Midi.toString(off.getMessage()) + "@" + off.getTick());
	}


}
