/* original source: https://github.com/SongProOrg/songpro-java  (MIT license) */
package net.judah.seq.chords;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.judah.gui.MainFrame;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@Data @AllArgsConstructor @NoArgsConstructor @EqualsAndHashCode(callSuper = true)
public class Section extends ArrayList<Chord> {
	@JsonIgnore
	private final UUID uuid = UUID.randomUUID();
	private String name = "main";
	private Part part = Part.MAIN;
	private final List<Directive> directives = new ArrayList<>();
	private Integer count; // total steps in section
	
	public Section(String name) {
		this.name = name;
	}

	@Override
	public boolean add(Chord e) {
		if (e.size() == 0)
			return false;
		return super.add(e);
	}
	
	public String dump() {
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

	public int getCount() {
		if (count == null) {
			int result = 0;
			for (Chord c : this)
				result += c.getSteps();
			count = result;
		}
		return count;
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
	
	public int getStepsAt(Chord chord) {
		int count = 0;
		for (Chord c : this) {
			if (c == chord)
				return count;
			count += c.getSteps();
		}
		throw new InvalidParameterException();
	}

	public void toggle(Directive d) {
		if (directives.contains(d))
			directives.remove(d);
		else 
			directives.add(d);
		MainFrame.update(this);
	}
		

	
	public boolean isOnLoop() {
		return directives.contains(Directive.LOOP);
	}
	
	@Override
	public String toString() {
		return name;
	}
}
