package net.judah.seq;

import javax.sound.midi.MidiEvent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public class MidiPair {

	private final MidiEvent on;
	private final MidiEvent off;
	
	@Override
	public int hashCode() {
		return on.hashCode() + (off == null ? 0 : off.hashCode());
	}

	@Override
	public boolean equals(Object o) {
		if (false == o instanceof MidiPair) return false;
    	MidiPair it = (MidiPair)o;
    	if (off == null) 
    		if (it.off != null) return false;
    		else return on.equals(it.on);
    	else if (it.off == null) return false;
    	else return on.equals(it.on) && off.equals(it.off);
	}
	
	
	
}
