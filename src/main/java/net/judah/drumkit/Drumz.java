package net.judah.drumkit;

import static judahzone.api.MidiConstants.DRUM_CH;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Drumz {

		D1("Drum1", DRUM_CH, "808", "Rock1"),
		D2("Drum2", DRUM_CH + 1, "Pearl", "Bossa1"),
		H1("Hats", DRUM_CH + 2, "Hats", "Hats1"),
		H2("Fills", DRUM_CH + 3, "VCO", "Fills1")
		;

		public final String name;
		public final int ch;
		public  final String program;
		public final String file;

}
