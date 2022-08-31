package net.judah.songs;

import java.io.File;

import net.judah.tracker.Track;
import net.judah.tracker.Tracker;
import net.judah.util.Constants;

/** Greetings, */ 
public class DrFalken extends SmashHit {

	@Override
	public void startup(Tracker tracks) {
		int delay = 50;
		for (Track t : tracks.getTracks())
			if (t.isSynth()) // keep drums running
				t.setActive(false);
		
		setFile(tracks.getDrum3(), new File(Track.DRUM_FOLDER, "SkipBeats"));
		Constants.timer(delay, () -> setFile(tracks.getDrum1(), new File(Track.DRUM_FOLDER, "Rock1")));
		Constants.timer(delay * 2, () -> setFile(tracks.getDrum2(), new File(Track.DRUM_FOLDER, "HiHats")));
		Constants.timer(delay * 3, () -> setFile(tracks.getBass(), new File(Track.MELODIC_FOLDER, "16ths")));
		Constants.timer(delay * 4, () -> setFile(tracks.getLead1(), new File(Track.MELODIC_FOLDER, "octave8ths")));
		Constants.timer(delay * 5, () -> setFile(tracks.getChords(), new File(Track.MELODIC_FOLDER, "arp2")));
		// reset looper ?
		resetChannels();
	}

	@Override
	public void cycle(Track t) {
	}

	@Override
	public void teardown() {
	}

}
