package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

public class MidiFile extends Sequence {
    
	public static final int TYPE_1 = 1;

	public MidiFile(int resolution) throws InvalidMidiDataException {
		super(Sequence.PPQ, resolution, 0);
	}

	public void setResolution(int rez) {
		if (rez > 0)
			resolution = rez;
	}
	
	int biggest() {
		int events = 0;
		int idx = 0;
		for (int i = 0; i < getTracks().length; i++)
			if (getTracks()[i].size() > events) {
				idx = i;
				events = getTracks()[i].size();
			}
		return idx;
	}
	
}
