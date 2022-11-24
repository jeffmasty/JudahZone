package net.judah.tracker;

import java.util.Arrays;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.midi.JudahClock;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;

@Getter 
public class SynthTracks extends Sequencers {

	private final PianoTrack lead1, lead2, bass, gm1, gm2, gm3; 
	private final Transpose transpose;
	
	public SynthTracks(JudahClock clock) {
		super(clock);
		transpose = new Transpose(clock);
		
		lead1 = new PianoTrack("Lead1", this, JudahZone.getSynth1(), 5);
		lead2 = new PianoTrack("Lead2", this, JudahZone.getSynth2(), 3);
		bass = new PianoTrack("Bass", this, JudahZone.getCrave(), 1);
		gm1 = new PianoTrack("GM1", this, JudahZone.getFluid(), 3);
		gm2 = new PianoTrack("GM2", this, JudahZone.getFluid(), 3);
		gm3= new PianoTrack("GM3", this, JudahZone.getFluid(), 3);

		gm1.setCh(1); 
		gm2.setCh(2); 
		gm3.setCh(3); 
		
		addAll(Arrays.asList(new PianoTrack[] {lead1, lead2, bass, gm1, gm2, gm3}));
	}
	
	public void checkLatch() {
		for (Track t : this) {
			if (((PianoTrack)t).isLatch()) {
				Transpose.setActive(true);
				RTLogger.log("Transpose", "MPK -> " + t.getName());
				return;
			}
		}
		Transpose.setActive(false);
	}

	@Override
	public void update(Track t) {
		super.update(t);
		t.getEdit().getMpk().setBackground(((PianoTrack)t).isLatch() ? Pastels.PINK : Pastels.BUTTONS);

	}
	
}
