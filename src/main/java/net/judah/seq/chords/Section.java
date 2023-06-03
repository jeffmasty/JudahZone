/* original source: https://github.com/SongProOrg/songpro-java  (MIT license) */
package net.judah.seq.chords;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@Data @AllArgsConstructor @NoArgsConstructor @EqualsAndHashCode(callSuper = true)
public class Section extends ArrayList<Chord> {
	
	public static enum Type { INTRO, VERSE, CHORUS, BRIDGE, ENDING, OTHER };
	
	private String name = "Main";
	private Type type;

	public Section(String name) {
		setName(name);
	}

	public void setName(String name) {
		for (Type t : Type.values())
			if (t.name().equalsIgnoreCase(name))
				type = t;
		this.name = name;
	}
	
	@Override
	public boolean add(Chord e) {
		if (e.size() == 0)
			return false;
		return super.add(e);
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		if (name != null && !name.isBlank())
			buf.append("--" + name + "-- ");
		int beats = 0;
		for(Chord chord : this) {
			beats += chord.getSteps();
			if (beats > 4 * 16) {
				buf.append(Constants.NL);
				beats = 0;
			}
			buf.append(chord.toString());
			buf.append(chord.getLyrics());
		}
		return buf.append(Constants.NL).toString();
	}

	public int stepCount() {
		int result = 0;
		for (Chord c : this)
			result += c.getSteps();
		return result;
	}

	public Chord getChordAt(int step) {
		
		
		
		int counter = 0;
		for (Chord c : this) {
			counter += c.getSteps();
			if (counter >= step)
				return c;
		}
		
		RTLogger.log(this, name + " No chord at " + step);
		
		return null;
	}

	public int steps() {
		int steps = 0;
		for (Chord c : this) 
				steps += c.getSteps();
		return steps;
	}
	
}
