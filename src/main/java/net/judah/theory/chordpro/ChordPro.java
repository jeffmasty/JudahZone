/* original source: https://github.com/SongProOrg/songpro-java  (MIT license) */

package net.judah.theory.chordpro;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@Setter @Getter
public class ChordPro {
	private final static Pattern SECTION_REGEX = Pattern.compile("#\\s*([^$]*)");
	private final static Pattern CHORDS_AND_LYRICS_REGEX = Pattern.compile("(\\[[\\w#b/]+])?([^\\[]*)", CASE_INSENSITIVE);
	private final static Pattern COMMENT_REGEX = Pattern.compile(">\\s*([^$]*)");

	private String title;
	private String artist;
	private String capo;
	private String key;
	private String tempo;
	private String year;
	private String album;
	private String tuning;
	private String comment;
	private String hyperlink;
	private final List<Section> sections = new ArrayList<>();

	public ChordPro() {
		String name = "/home/judah/temp/Dont-Know-Why.pro";
		List<String> lines = readFile(name);
		parse(lines);
		RTLogger.log(this, "Sections: " + sections.size());
		RTLogger.log(this, this.toString());
	}

	private List<String> readFile(String songPath) {
        List<String> songLines = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(songPath))) {

            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                songLines.add(sCurrentLine);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return songLines;
    }

  private void parse(List<String> lines) {
	  
    Section currentSection = null;

    for (String text : lines) {
    	if (text.startsWith("http")) {
    		hyperlink = text;
    		continue;
    	}
    	else if (text.startsWith("{")) {
    		processAttribute(text);
    	}
    	else if (text.startsWith("#")) {
    		currentSection = processSection(text);
    	} else {
    		processLyricsAndChords(currentSection, text);
      }
    }
  }

    private void processAttribute(String line) {
    	String[] split = line.split(":");
    	String key = split[0].replace("{", "").trim();
    	String value = split[1].replace("}", "").trim();
    	
    	switch(key) {
    	case "t": case "title":
    		title = value; break;
    	case "a": case "artist":
    		artist = value; break;
    	case "key":
    		key = value; break;
    	case "c": case "comment":
    		comment = value; break;
    	case "tuning":
    		tuning = value; break;
    	case "y": case "year":
    		year = value; break;
    	case "album": 
    		album = value; break;
    	case "tempo":
    		tempo = value; break;
    	}
    }

	private Section processSection(String text) {
		Matcher matcher = SECTION_REGEX.matcher(text);

		Section currentSection = new Section("");

		if (matcher.matches()) {
			String name = matcher.group(1).trim();
			currentSection.setName(name);
			sections.add(currentSection);
		}

		return currentSection;
	}

	private void processLyricsAndChords(Section currentSection, String text) {
		if (text.isEmpty()) {
			return;
		}

		if (currentSection == null) {
			currentSection = new Section("");
			sections.add(currentSection);
		}

		Line line = new Line();

		if (text.startsWith("|-")) {
			line.setTablature(text);
		} else if (text.startsWith(">")) {
			Matcher matcher = COMMENT_REGEX.matcher(text);

			if (matcher.matches()) {
				String comment = matcher.group(1).trim();
				line.setComment(comment);
			}
		} else {
			Matcher matcher = CHORDS_AND_LYRICS_REGEX.matcher(text);

			while (matcher.find()) {
				if (matcher.groupCount() == 2) {
					Part part = new Part();

					if (matcher.group(1) != null) {
						part.setChord(matcher.group(1).trim().replace("[", "").replace("]", ""));
					} else {
						part.setChord("");
					}

					if (!matcher.group(2).equals("")) {
						part.setLyric(matcher.group(2));
					} else {
						part.setLyric("");
					}

					if (!(part.getChord().equals("") && part.getLyric().equals(""))) {
						line.getParts().add(part);
					}
				}
			}
		}

		currentSection.getLines().add(line);
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer(Constants.NL);
		if (title != null) 
			buf.append("Title: ").append(title).append("  ");
		if (artist != null)
			buf.append("Artist: ").append(artist).append("  ");
		buf.append(Constants.NL);
		sections.forEach(s->buf.append(s.toString()));
		return buf.toString();
	}
	
}
