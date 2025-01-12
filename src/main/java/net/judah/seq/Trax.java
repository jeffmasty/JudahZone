package net.judah.seq;

import static net.judah.seq.MidiConstants.DRUM_CH;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum Trax {

	D1("Drum1", DRUM_CH),
	D2("Drum2", DRUM_CH + 1),
	H1("Hats", DRUM_CH + 2),
	H2("Fills", DRUM_CH + 3),

	B("Bass", 0),
	TK1("S.One", 0),
	TK2("S.Two", 0),
	F1("Fluid1", 1),
	F2("Fluid2", 2),
	F3("Fluid3", 3)
	;

	private final String name;
	private final int ch;

	public static final Trax[] drums = {D1, D2, H1, H2};
	public static final Trax[] tacos = {TK1, TK2};
	public static final Trax[] fluids = {F1, F2, F3};

}
