package net.judah.seq;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter @EqualsAndHashCode
public class Edit {
	public static enum Type {
		NEW, DEL, TRANS, LENGTH, REMAP, TRIM, INS
	}

	private final Type type;
	private final ArrayList<MidiPair> notes = new ArrayList<>();
	@Setter private Prototype destination;
	@Setter private Prototype origin;

	/** index into notes array that relates to destination */
	@Setter private int idx;

	public Edit (Type t, MidiPair... x) {
		type = t;
		for (MidiPair p : x)
			notes.add(p);
	}

	public Edit(Type t, Vector<MidiEvent> remap) { // drums
		type = t;
		for (MidiEvent e : remap)
			notes.add(new MidiPair(e, null));
	}

	public Edit(Type t, List<MidiPair> notes) {
		this.type = t;
		this.notes.addAll(notes);
	}

	public void setDestination(Prototype d, Prototype source) {
		destination = d;
		for (int i = 0; i < notes.size(); i++) {
			MidiEvent on = notes.get(i).getOn();
			if (on.getTick() == source.tick && ((ShortMessage)on.getMessage()).getData1() == source.data1) {
				idx = i;
				break;
			}
		}
	}

}
