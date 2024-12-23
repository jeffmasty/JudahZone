package net.judah.seq.chords;

import lombok.Getter;

@Getter
public enum Scale {

	MAJOR("Major", 2,2,1,2,2,2),
	DOM7("Dominant 7", 2,2,1,2,2,1),
	MINOR("Dorian", 2,1,2,2,2,1),
	BLUES("Blues", 3,2,1,1,3),
	BEPOP("Bepop", 2,2,1,1,1,2,2),
	BEPOP7("Bepop", 2,2,1,1,1,2,1),
	MAJOR5("Major Pentatonic", 2,2,3,2),
	MINOR5("Minor Pentatonic", 3,2,2,3),
	HARMONIC("Harmonic Minor", 2,1,2,2,1,3),
	MM_ASC("Melodic Min. Up", 2,1,2,2,2,2),
	MM_DEC("Melodic Min. Down", 2,1,2,2,1,2),
	CHROMATIC("Chromatic", 1,1,1,1,1,1,1,1,1,1,1,1),
	;

	private final String tooltip;
	private final int[] intervals;

	Scale(String tip, int... x) {
		tooltip = tip;
		intervals = x;
	}

}
// https://github.com/sangervu/MusicScales/blob/master/MusicScales/src/musicscales/MusicScales.java
//    HARMONIC = "2122131";
//    MELODIC = "2122221";
//    IONIAN = "2212221";
//    DORIAN = "2122212";
//    PHRYGIAN = "1222122";
//    LYDIAN = "2221221";
//    MIXOLYDIAN = "2212212";
//    AEOLIAN = "2122122";
//    LOCRIAN = "1221222";