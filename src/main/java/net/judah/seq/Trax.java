package net.judah.seq;

import static net.judah.seq.MidiConstants.DRUM_CH;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum Trax {
	D1("Drum1", DRUM_CH, "Pearl", "Rock1"),
	D2("Drum2", DRUM_CH + 1, "808", "Bossa1"),
	H1("Hats", DRUM_CH + 2, "Hats", "Hats1"),
	H2("Fills", DRUM_CH + 3, "VCO", "Fills1"),

	B("Bass", 0, "", "Bass2"),
	TK1("S.One", 0, "Drops1", "8ths"),
	TK2("S.Two", 0, "FeelGood", "16ths"),
	F1("Fluid1", 1, "Strings", "0s"),
	F2("Fluid2", 2, "Palm Muted Guitar", "CRDSKNK"),
	F3("Fluid3", 3, "Harp", "8ths")
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
