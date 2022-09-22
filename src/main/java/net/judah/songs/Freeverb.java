package net.judah.songs;

import java.io.File;

import net.judah.MainFrame;
import net.judah.looper.Looper;
import net.judah.mixer.Channels;
import net.judah.tracker.Track;
import net.judah.tracker.JudahBeatz;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** Greetings, */ 
public class Freeverb extends SmashHit {

	@Override
	public void startup(JudahBeatz tracks, Looper loops, Channels ch, MainFrame frame) {
		super.startup(tracks, loops, ch, frame);
		int delay = 333;
		for (Track t : tracks.getTracks())
			if (t.isSynth()) // keep drums running
				t.setActive(false);
		
		setFile(tracks.getDrum3(), new File(Track.DRUM_FOLDER, "SkipBeats"));
		Constants.timer(delay, () -> setFile(tracks.getDrum1(), new File(Track.DRUM_FOLDER, "Rock1")));
		Constants.timer(delay * 2, () -> setFile(tracks.getDrum2(), new File(Track.DRUM_FOLDER, "HiHats")));
		Constants.timer(delay * 3, () -> setFile(tracks.getBass(), new File(Track.MELODIC_FOLDER, "16ths")));
		Constants.timer(delay * 4, () -> setFile(tracks.getLead1(), new File(Track.MELODIC_FOLDER, "octave8ths")));
		Constants.timer(delay * 5, () -> setFile(tracks.getChords(), new File(Track.MELODIC_FOLDER, "arp2")));
        Constants.timer(delay * 6, () -> RTLogger.log(this, "GREETINGS PROF FALKEN."));

		// reset looper ?
		resetChannels();
		ch.initVolume();
        ch.initMutes();

	}

}
