package net.judah.songs;

import static net.judah.JudahZone.*;

import java.io.File;

import net.judah.MainFrame;
import net.judah.looper.Looper;
import net.judah.mixer.Channels;
import net.judah.mixer.Instrument;
import net.judah.tracker.Cycle;
import net.judah.tracker.Track;
import net.judah.tracker.JudahBeatz;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class Sleepwalk extends SmashHit {
	int count;
	boolean odd;
	Track track;
	
	@Override
	public void startup(JudahBeatz t, Looper loops, Channels ch, MainFrame frame) {
		super.startup(t, loops, ch, frame);
		getClock().setLength(4);
		track = t.getBass();
		track.setFile(new File(track.getFolder(), "Sleepwalk"));
		track.getCycle().setCustom(this);
		track.setActive(true);
		Instrument guitar = getInstruments().getGuitar();
		guitar.getReverb().setActive(false);
		guitar.getLatchEfx().latch(getLooper().getLoopA());
		count = 0;
		getFrame().sheetMusic(new File(Constants.SHEETMUSIC, "Sleepwalk.png"));
	}

	@Override
	public void cycle(Track t) {
		if (Cycle.isVerse()) {
				if (Cycle.isTrigger()) {
					Cycle.setTrigger(false);
					if (count == 4) {
						track.setCurrent(track.get(0));
						Cycle.setVerse(false);
						count = 0;
						odd = false;
						getLooper().getLoopA().setTapeCounter(0);
						getLooper().getLoopA().setOnMute(false);
						return;
					} 
					if (count == 0) {
						RTLogger.log(this, "Sleepwalk verse");
						getLooper().getLoopA().setOnMute(true);
						track.setCurrent(track.get(2));
					}
					else if (count == 1)
						track.setCurrent(track.get(3));
					else if (count == 2)
						track.setCurrent(track.get(2));
					else if (count == 3)
						track.setCurrent(track.get(4));
					
				}
				else {
					count++;
					Cycle.setTrigger(true);
				}
				return;
			}
			else {
				track.next(!odd);
				odd = !odd;
			}					
		
	}

	@Override
	public void teardown() {
		track.clearFile();
		track.getCycle().setCustom(null);
	}

}
