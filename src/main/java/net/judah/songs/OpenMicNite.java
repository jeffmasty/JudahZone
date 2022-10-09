package net.judah.songs;

import java.io.File;

import net.judah.JudahZone;
import net.judah.tracker.Track;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** Greetings, */ 
public class OpenMicNite extends SmashHit {

	@Override
	public void startup() {
		super.startup();
		int delay = 333;
		for (Track t : notes) // keep beats running
			t.setActive(false);
		
		setFile(drum2, new File(Track.DRUM_FOLDER, "SkipBeats"));
		Constants.timer(delay, () -> setFile(drum1, new File(Track.DRUM_FOLDER, "Rock1")));
		Constants.timer(delay * 2, () -> setFile(drum2, new File(Track.DRUM_FOLDER, "HiHats")));
		Constants.timer(delay * 3, () -> setFile(bass, new File(Track.MELODIC_FOLDER, "16ths")));
		Constants.timer(delay * 4, () -> setFile(lead2, new File(Track.MELODIC_FOLDER, "octave8ths")));
		Constants.timer(delay * 5, () -> setFile(chords, new File(Track.MELODIC_FOLDER, "arp2")));
        Constants.timer(delay * 6, () -> RTLogger.log(this, "GREETINGS PROF FALKEN."));

		// reset looper ?
		resetChannels();
		JudahZone.getNoizeMakers().initVolume();
        JudahZone.getNoizeMakers().initMutes();

	}

}
