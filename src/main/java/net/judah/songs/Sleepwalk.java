package net.judah.songs;

import java.io.File;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.mixer.LineIn;
import net.judah.settings.Channels;
import net.judah.tracker.Cycle;
import net.judah.tracker.Track;
import net.judah.tracker.Tracker;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class Sleepwalk extends SmashHit {
	int count;
	boolean odd;
	Track track;
	
	@Override
	public void startup(Tracker t, Looper loops, Channels ch) {
		JudahClock.getInstance().setLength(4);
		track = t.getBass();
		track.setFile(new File(track.getFolder(), "Sleepwalk"));
		track.getCycle().setCustom(this);
		track.setActive(true);
		LineIn guitar = JudahZone.getChannels().getGuitar();
		guitar.getReverb().setActive(false);
		guitar.getLatchEfx().latch(JudahZone.getLooper().getLoopA());
		count = 0;
		MainFrame.get().sheetMusic(new File(Constants.SHEETMUSIC, "Sleepwalk.png"));
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
						JudahZone.getLooper().getLoopA().setTapeCounter(0);
						JudahZone.getLooper().getLoopA().setOnMute(false);
						return;
					} 
					if (count == 0) {
						RTLogger.log(this, "Sleepwalk verse");
						JudahZone.getLooper().getLoopA().setOnMute(true);
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
