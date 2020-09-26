package net.judah.util;

import lombok.Getter;
import net.judah.song.Edits;

public class Clipboard {

	@Getter private static Edits mostRecent;
	
	public static void clicker(Edits editable) {
		mostRecent = editable;
	}
	
}
