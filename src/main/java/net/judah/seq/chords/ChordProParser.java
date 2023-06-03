/* original source: https://github.com/SongProOrg/songpro-java  (MIT license) */

package net.judah.seq.chords;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.judah.util.Constants;

@Setter @Getter
public class ChordProParser {
	
	@NoArgsConstructor @AllArgsConstructor @Getter
	class Pair { 
		String chord; 
		String lyrics = "";
		Pair(String chord) { this.chord = chord;}
	}
	
	private final static Pattern COMMENT_REGEX = Pattern.compile(">\\s*([^$]*)");
	private static final String SECTION_START = "{start_of_";
	private static final String SECTION_END = "{end_of_";
	
	ChordProData data = new ChordProData();
	private final List<Section> sections = new ArrayList<>();
	private final int steps;
	private Section current;
	
	public static ChordProParser load(File file, int steps) {
		ChordProParser result = null;
		if (file != null && file.isFile()) 
			result = new ChordProParser(file, steps);
		return result;
	}
	
	private ChordProParser(File file, int steps) {
		this.steps = steps;
		parse(readFile(file.getAbsolutePath()));
	}

	private List<String> readFile(String songPath) {
        List<String> songLines = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(songPath))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                songLines.add(sCurrentLine.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return songLines;
    }

	private void parse(List<String> lines) {

		current = new Section("");
		sections.add(current);

		for (String text : lines) {
			if (text.isBlank())
				continue;
			if (text.startsWith("http")) {
				data.hyperlink = text;
				continue;
			} else if (text.startsWith(SECTION_START)) {
				processSection(text);
			} else if (text.startsWith(SECTION_END)) {
				current = new Section("");
				sections.add(current);
			} else if (text.startsWith("{")) {
				processAttribute(text);
			} else {
				if (text.startsWith("|-"))
					continue;
				else if (text.startsWith(">")) {
					Matcher matcher = COMMENT_REGEX.matcher(text);
					if (matcher.matches())
						continue;
				} else
					processChords(text);
			}
		}
	}

	private void processAttribute(String line) {
		String[] split = line.split(":");
		String key = split[0].replace("{", "").trim();
		String value = split[1].replace("}", "").trim();

		
		switch (key) {
		case "t":
		case "title":
			data.title = value;
			break;
		case "a":
		case "artist":
			data.artist = value;
			break;
		case "key":
			data.key = value;
			break;
		case "c":
		case "comment":
			data.comment = value;
			break;
		case "tuning":
			data.tuning = value;
			break;
		case "y":
		case "year":
			data.year = value;
			break;
		case "album":
			data.album = value;
			break;
		case "tempo":
			data.tempo = value;
			break;
		}
	}

	private void processSection(String text) {
		int idx = text.lastIndexOf('_');

		String name = text.substring(idx + 1).replace("}", "");
		if (current.isEmpty() && current.getName().isEmpty())
			current.setName(name);
		else {
			current = new Section(name);
			sections.add(current);
		}
	}

	private void processChords(String text) {
		if (text.isEmpty())
			return;

		String[] split = text.split("\\|");
		@SuppressWarnings("unchecked")
		ArrayList<Pair>[] parts = new ArrayList[split.length];
		for (int i = 0; i < split.length; i++) {
			parts[i] = subtext(split[i]);
			if (parts[i].size() != 2 || (
					i == 0 && text.indexOf('|') > text.indexOf('['))) {
				for (Pair p : parts[i]) 
					current.add(new Chord(p.chord, p.lyrics, steps));
			}
			else { // Only handles up to 2 chords per bar for now
				Pair a = parts[i].get(0);
				Pair b = parts[i].get(1);
				int first = steps / 2;
				current.add(new Chord(a.chord, a.lyrics, first));
				current.add(new Chord(b.chord, b.lyrics, steps - first));
			}
		}
	}
	private ArrayList<Pair> subtext(String text) {
		ArrayList<Pair> result = new ArrayList<>();
		// character by character creating parts, chords and lyrics
		Pair part = new Pair();
		int idx = 0;
		int length = text.length();
		while (idx < length) {
			char c = text.charAt(idx);
			if (c == '[') {
				if (part.getChord() != null) {
					result.add(part);
					part = new Pair();
				}
				int end = text.indexOf(']', ++idx);
				if (idx >= 0) {
					part = new Pair(text.substring(idx, end));
					idx = end;
				}
			} 
			else if (c == '/' && idx + 1 < length && text.charAt(idx + 1) == '/' && part.getChord() != null) {
				result.add(part); // duplicate
				part = new Pair(part.getChord());
				idx++;
			}
			else if (part.getChord() == null) // cat lyrics to previous...
				if (current.isEmpty()) {
					int sec = sections.indexOf(current);
					if (sec != 0) { // cat to previous section
						Section section = sections.get(sec - 1);
						Chord previous = section.get(section.size()-1);
						previous.setLyrics(previous.getLyrics() + c);
					}
				}
				else { // cat to previous chord
					Chord previous = current.get(current.size() - 1);
					previous.setLyrics(previous.getLyrics() + c);
				}
			else 
				part.lyrics = part.lyrics + c;
			idx++;
		}
		
		if (part.getChord() != null)
			result.add(part);
		return result;

	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer(Constants.NL);
		if (data.title != null) 
			buf.append("Title: ").append(data.title).append("  ");
		if (data.artist != null)
			buf.append("Artist: ").append(data.artist).append("  ");
		buf.append(Constants.NL);
		sections.forEach(s->buf.append(s.toString()));
		return buf.toString();
	}

}
