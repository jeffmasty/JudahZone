package net.judah.util;

import lombok.Data;
import net.judah.sequencer.editor.Edits.Copyable;

@Data
public class KeyPair implements Copyable {

	private final String key;
	private final Object value;

	@Override
	public KeyPair clone() throws CloneNotSupportedException {
		
		return new KeyPair(key, value);
	}
	
}
