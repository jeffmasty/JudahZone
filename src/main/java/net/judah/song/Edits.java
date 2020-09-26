package net.judah.song;

import java.util.List;

public interface Edits {
	
	public static interface Copyable extends Cloneable{
		public Copyable clone() throws CloneNotSupportedException;
	}

	void editAdd();
	
	void editDelete();
	
	List<Copyable> copy();
	
	List<Copyable> cut();
	
	void paste(List<Copyable> clipboard);
	
	
	/////////////////////
//	public List<Object> copy() {
//		
//		return null;
//	}
//	
//	public List<Object> cut() {
//		return null;
//	}
//	
//	public void paste(List<Object> clipboard) {
//		
//	}
//	
//	public void editAdd() {
//		
//	}
//	
//	public void editDelete() {
//		
//	}

	
	
	
}
