package net.judah.seq;

import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter @EqualsAndHashCode
public class Edit {
	public static enum Type { 
		NEW, DEL, TRANS, GAIN
	}
	
	private final Type type;
	private final ArrayList<MidiPair> notes = new ArrayList<>();
	@Setter private Prototype destination;
	
	public Edit (Type t, MidiPair... x) {
		type = t;
		for (MidiPair p : x)
			notes.add(p);
	}
	
	public Edit(Type t, List<MidiPair> notes) {
		this.type = t;
		this.notes.addAll(notes);
	}

}
