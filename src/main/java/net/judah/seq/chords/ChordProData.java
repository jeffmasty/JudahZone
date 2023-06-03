package net.judah.seq.chords;

import lombok.Data;

@Data
public class ChordProData {
	boolean active;
	String title;
	String artist;
	String capo;
	String key;
	String tempo;
	String year;
	String album;
	String tuning;
	String comment;
	String hyperlink;
}
