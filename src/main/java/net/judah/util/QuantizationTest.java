package net.judah.util;

import net.judah.sequencer.MidiEvent;
import net.judah.sequencer.MidiTrack;
import net.judah.sequencer.Quantize;

public class QuantizationTest {

	/**@throws JudahException if test fails */
	public QuantizationTest() throws JudahException {
		
		int[] input = new int[] {33, 76, 115, 150, 190 };
		// int[] expected = new int[] {50, 100, 100, 150, 200}; quarter notes
		int[] expected = new int[] {25, 75, 125, 150, 199}; // eighth notes
		
		MidiTrack track = new MidiTrack();
		int beats = 4;

		track.setLength(200);
		track.setQuantize(Quantize.EIGHTH);

		long unit = track.getLength() / (beats * 2); // *2 for eighth notes
		
		for (int i = 0; i < input.length; i++) {
			MidiEvent e = new MidiEvent(Constants.BASSDRUM, 0);
			e.setOffset(input[i]);
			e.quantize(unit, track.getLength());
			if (e.getOffset() != expected[i])
				throw new JudahException("input: " + input[i] + " expected: " + expected[i] 
						+ " got: " + e.getOffset());
		}
	}
	
	public static void main(String[] args) {
		try {
			new QuantizationTest();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
}
