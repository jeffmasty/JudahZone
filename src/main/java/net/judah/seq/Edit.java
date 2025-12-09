package net.judah.seq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.judah.midi.Midi;

@Getter @EqualsAndHashCode
public class Edit {
	public static enum Type {
		NEW, DEL, TRANS, LENGTH, REMAP, TRIM, INS, MOD
	}

	private final Type type;
	private final ArrayList<MidiEvent> notes = new ArrayList<>();
	@Setter private Prototype destination;
	@Setter private Prototype origin;

	/** index into notes array that relates to destination */
	@Setter private int idx;

	public Edit (Type t, MidiEvent... x) {
		type = t;
		for (MidiEvent evt : x)
			notes.add(evt);
	}

	public Edit(Type t, List<MidiNote> list) {
		type = t;
		for (MidiNote p : list) {
			notes.add(new MidiEvent(p.getMessage(), p.getTick()));
			if (p.getOff() != null)
				notes.add(new MidiEvent(p.getOff().getMessage(), p.getOff().getTick()));
		}
	}

	public Edit(Type t, Collection<MidiEvent> remap) { // drums
		type = t;
		for (MidiEvent e : remap)
			notes.add(e);
	}

//	public Edit(Type t, List<MidiPair> incoming) {
//		this.type = t;
//		for (MidiPair p : incoming) {
//			notes.add(p.getOn());
//			if (p.getOff() != null)
//				notes.add(p.getOff());
//		}
//	}

	public void setDestination(Prototype d, Prototype source) {
		destination = d;
		for (int i = 0; i < notes.size(); i++) {
			MidiEvent on = notes.get(i);
			if (Midi.isNoteOn(on.getMessage()) && on.getTick() == source.tick && ((ShortMessage)on.getMessage()).getData1() == source.data1) {
				idx = i;
				break;
			}
		}
	}

}
