package net.judah.seq;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter @EqualsAndHashCode
public class Edit {
	public static enum Type { 
		NEW, DEL, MOVE
	}
	
	private final Type type;
	private final ArrayList<MidiPair> notes = new ArrayList<>();
	@Setter private Point origin;
	
	public Edit(Type t, List<MidiPair> notes) {
		this.type = t;
		this.notes.addAll(notes);
	}

}
