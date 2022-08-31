package net.judah.sequencer.editor;

import java.util.List;

public interface Edits {
	
	public static interface Copyable extends Cloneable {
		public Copyable clone() throws CloneNotSupportedException;
	}

	void editAdd();
	
	void editDelete();
	
	List<Copyable> copy();
	
	List<Copyable> cut();
	
	void paste(List<Copyable> clipboard);
	
}
