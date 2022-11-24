package net.judah.tracker;

import java.util.Arrays;

import lombok.Getter;
import net.judah.drumz.DrumMachine;
import net.judah.midi.JudahClock;

@Getter
public class DrumTracks extends Sequencers {

	private final DrumTrack drum1, drum2, hats, fills;
	
	// TODO CRUD tracks
	public DrumTracks(JudahClock clock, DrumMachine drumz) {
		super(clock);

		drum1 = new DrumTrack(clock, "Drum1", drumz.getDrum1(), this);
		drum2 = new DrumTrack(clock, "Drum2", drumz.getDrum2(), this);
		hats = new DrumTrack(clock, "HiHats", drumz.getHats(), this);
		fills = new DrumTrack(clock, "Fillz", drumz.getFills(), this);
		
		addAll(Arrays.asList(new DrumTrack[] {drum1, hats, drum2, fills}));

	}

}
