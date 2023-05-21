package net.judah.seq;

import javax.sound.midi.MidiEvent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.midi.Midi;

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
	
	@Override
	public String toString() {
		return "on:" + Midi.toString(on.getMessage()) + "@" + on.getTick() + " off:" + (off == null ? "null" :
			Midi.toString(off.getMessage()) + "@" + off.getTick());
	}

	public MidiPair(MidiPair p) {
		on = new MidiEvent(p.getOn().getMessage(), p.getOn().getTick());
		if (p.getOff() == null)
			off = null;
		else
			off = new MidiEvent(p.getOff().getMessage(), p.getOff().getTick());
	}
	
}
