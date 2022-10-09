package net.judah.tracker;

import java.util.Arrays;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.midi.JudahClock;
import net.judah.util.RTLogger;

@Getter 
public class JudahNotez extends Trax {

	private final PianoTrack lead1, lead2, chords, bass;
	
	
	public JudahNotez(JudahClock clock) {
		super(clock);
		lead1 = new PianoTrack("Lead1", this, JudahZone.getSynth1(), 5);
		lead2 = new PianoTrack("Lead2", this, JudahZone.getSynth2(), 3);
		chords = new PianoTrack("Chords", this, JudahZone.getFluid(), 3);
		chords.setCh(1); 
		bass = new PianoTrack("Bass", this, JudahZone.getCrave(), 1);
		
		addAll(Arrays.asList(new PianoTrack[] {lead1, lead2, chords, bass}));
	}
	
	public void checkLatch() {
		for (Track t : this) {
			if (t.isLatch()) {
				Transpose.setActive(true);
				RTLogger.log("Transpose", "MPK -> " + t.getName());
				return;
			}
		}
		Transpose.setActive(false);
	}

	public MidiCable createMidiCable(TrackView t) {
		return new MidiCable(t, this);
	}
	
}
