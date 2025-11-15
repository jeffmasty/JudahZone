package net.judah.seq;

import static net.judah.seq.MidiConstants.DRUM_CH;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum Trax {
	D1("Drum1", DRUM_CH, "808", "Rock1"),
	D2("Drum2", DRUM_CH + 1, "Pearl", "Bossa1"),
	H1("Hats", DRUM_CH + 2, "Hats", "Hats1"),
	H2("Fills", DRUM_CH + 3, "VCO", "Fills1"),

	B("Bass", 0, "", "Bass2"),
	TK1("Taco", 0, "Drops1", "test"),
	TK2("Tk2", 0, "FeelGood", "16ths"),
	F1("Fluid1", 0, "Strings", "0s"),
	F2("Fluid2", 1, "Palm Muted Guitar", "CRDSKNK"),
	F3("Fluid3", 2, "Harp", "8ths")
	;

	private final String name;
	private final int ch;
	private final String program;
	private final String file;

	public static final Trax[] drums = {D1, D2, H1, H2};
	public static final Trax[] tacos = {TK1, TK2};
	public static final Trax[] fluids = {F1, F2, F3};
	public static final Trax[] pianos = {B, TK1, TK2, F1, F2, F3};

}
