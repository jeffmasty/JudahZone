package net.judah.seq;

import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

public class Accumulator extends ArrayList<MidiEvent> {

	public MidiEvent get(ShortMessage m) {
		int data1 = m.getData1();
		MidiEvent result = null;
		for (MidiEvent e : this)
			if (((ShortMessage)e.getMessage()).getData1() == data1) {
				result = e;
				break;
			}
		if (result != null)
			remove(result);
		return result;
	}
	
}
