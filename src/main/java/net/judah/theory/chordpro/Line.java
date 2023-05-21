/* original source: https://github.com/SongProOrg/songpro-java  (MIT license) */
package net.judah.theory.chordpro;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import net.judah.theory.Chord;
import net.judah.util.Constants;

@Setter @Getter
public class Line {
	private final List<Part> parts = new ArrayList<>();
	private String comment;
	private String tablature;

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Part p : parts) {
			if (p.getChord() != null && p.getChord().length() > 0) 
				buf.append(new Chord(p.getChord()).toString());
			buf.append(p.getLyric());
		}
		return buf.append(Constants.NL).toString();
	}
}
