package net.judah.songs;

import static net.judah.JudahZone.getLooper;

import net.judah.tracker.Cycle;
import net.judah.tracker.Track;
import net.judah.tracker.Track.Cue;
import net.judah.util.RTLogger;

public class Sleepwalk extends SmashHit {
	int count;
	boolean odd;
	
	@Override
	public void startup() {
		clock.setLength(4);
		clock.writeTempo(93);
		crave.setMuteRecord(true);

		bass.setFile("Sleepwalk");
		bass.getCycle().setCustom(this);
		bass.setActive(true);
		lead2.setActive(false);
		lead2.setFile("SleepArp");
		lead2.setCue(Cue.Bar);
		synth2.getPresets().load("Drops1");
		guitar.getReverb().setActive(false);
		guitar.getLatchEfx().latch(getLooper().getLoopA());
		frame.sheetMusic("Sleepwalk.png");
	}

	@Override
	public void cycle(Track t) {
		if (Cycle.isVerse()) {
				if (Cycle.isTrigger()) {
					Cycle.setTrigger(false);
					if (count == 4) {
						bass.setCurrent(bass.get(0));
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
						bass.setCurrent(bass.get(2));
					}
					else if (count == 1)
						bass.setCurrent(bass.get(3));
					else if (count == 2)
						bass.setCurrent(bass.get(2));
					else if (count == 3)
						bass.setCurrent(bass.get(4));
					
				}
				else {
					count++;
					Cycle.setTrigger(true);
				}
				return;
			}
			else {
				bass.next(!odd);
				odd = !odd;
			}					
		
	}

	@Override
	public void teardown() {
		bass.clearFile();
		bass.getCycle().setCustom(null);
	}

}
