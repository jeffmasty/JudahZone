package net.judah.seq.chords;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public class ParseException extends Exception {

	private final Throwable t;
	private final String line;
	
	@Override
	public String getMessage() {
		return line + " " + t.getClass().getSimpleName() + ": " + t.getMessage();
	}
}
