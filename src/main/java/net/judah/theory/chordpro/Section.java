/* original source: https://github.com/SongProOrg/songpro-java  (MIT license) */
package net.judah.theory.chordpro;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @Getter @NoArgsConstructor
public class Section {
	private String name;
	private final List<Line> lines = new ArrayList<>();

	public Section(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		lines.forEach(l -> buf.append(l.toString()));
		return buf.toString();
	}

}
