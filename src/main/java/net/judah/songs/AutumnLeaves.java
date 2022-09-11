package net.judah.songs;

import net.judah.MainFrame;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.looper.Looper;
import net.judah.settings.Channels;
import net.judah.tracker.Cycle;
import net.judah.tracker.Track;
import net.judah.tracker.Track.Cue;
import net.judah.tracker.Tracker;
import net.judah.util.Constants;

public class AutumnLeaves extends SmashHit {
	boolean introDone;
	boolean vampDone;
	int count;
	
	Track bass;
	Track drums;
	final TimeListener duplicate = new TimeListener() {
		@Override public void update(Property prop, Object value) {
			if (value == Status.TERMINATED) {
				loops.getLoopA().removeListener(this);
				Constants.timer(50, () ->loops.getLoopC().duplicate()); // make (B)ridge 2x as long as loop A
			}
	}};

	@Override
	public void startup(Tracker t, Looper loops, Channels ch) {
		super.startup(t, loops, ch);
		/*  
		 | Am7   | D7    |   Gmaj7 | Cmaj7   | F#-7b5  | B7     | E-     |      |   
(x2)	 | Am7   | D7    |   Gmaj7 | Cmaj7   | F#-7b5  | B7     | E-     |      |   
		 
		 | F#-7b5| B7    |  E-     | E-      | A-7     | D7     | Gmaj7  | 	    |
		 | F#-7b5| B7    |  E- Eb7 | D-7 Db7 | Cmaj7   | B7		| E-	 |		|
		 
		 */

		drums = t.getDrum1();
		bass = t.getBass();
		
		t.getClock().end();
		setupDrums();
		setupBass();
		t.getClock().writeTempo(93);
		t.getClock().setLength(8);
		MainFrame.get().sheetMusic("AutumnLeaves.png");
		
		loops.getLoopA().addListener(duplicate);
		loops.getLoopB().setArmed(true); 
		loops.getLoopC().setOnMute(true);
		
		drums.setActive(true);
		t.getClock().begin();
		Cycle.setTrigger(false);
		Cycle.setVerse(false);
		count = 0;
	}
	
	private void setupBass() {
		bass.setFile("AutumnB"); // cred: https://www.youtube.com/watch?v=pS7v0UEFcps
		bass.getEdit().getRatio().setSelectedIndex(2);
		bass.setCue(Cue.Bar);
		bass.getCycle().setCustom(this);
		bass.setCurrent(bass.get(1));
		count = 1;
	}
	
	private void setupDrums() {
		drums.setFile("Bossa1");
	}
	@Override
	public void cycle(Track t) {
		if (Cycle.isTrigger()) {
			Cycle.setTrigger(false);
			bass.setPattern("Bridge1");
			count = 0;
		}
		else if (Cycle.isVerse()) {
			if (++count == 4) {
				count = 0;
				bass.setCurrent(bass.get(count));
				Cycle.setVerse(false);
			}
			else
				bass.next(true);
		}
		else if (++count == 2) {
				count = 0;
				bass.setCurrent(bass.get(count));
		} else 
				bass.next(true);
	}
	
	@Override
	public void teardown() {
		bass.getCycle().setCustom(null);
		loops.getLoopA().removeListener(duplicate);
	}
	
}
