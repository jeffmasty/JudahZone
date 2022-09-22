package net.judah.songs;


import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.looper.Looper;
import net.judah.mixer.Channels;
import net.judah.tracker.Cycle;
import net.judah.tracker.Pattern;
import net.judah.tracker.Track;
import net.judah.tracker.JudahBeatz;
import net.judah.util.RTLogger;

public class StolenMoments extends SmashHit {

	@Override
	public void startup(JudahBeatz t, Looper loops, Channels ch, MainFrame frame) {
		super.startup(t, loops, ch, frame);
		frame.sheetMusic("StolenMoments.jpg");
		t.getDrum1().setFile("StolenMoments");
		t.getDrum1().setActive(true);
		t.getDrum1().getCycle().setCustom(this);
		t.getClock().setLength(16);
		t.getClock().writeTempo(110);
		loops.clear();
		loops.getLoopA().setOnMute(false);
		ch.getGuitar().setPreset(JudahZone.getPresets().byName("Dist"));
		ch.getGuitar().setPresetActive(false);
		ch.getGuitar().getLatchEfx().latch(loops.getLoopA());
		RTLogger.log(this, "outro trigger");
	}

	@Override
	public void cycle(Track track) {
		if (Cycle.isTrigger()) {
			Pattern outro = track.get(track.size() - 1);
			if (track.getCurrent().equals(outro)) {
				t.getClock().end();
				JudahZone.nextSong();
			}
			else { // start outro
				t.getDrum2().setActive(false); // stop hihats
				track.setCurrent(outro);
			}
			return;
		}
		int i = track.indexOf(track.getCurrent()) + 1;
		if (i >= track.size() - 1)
			i = 1;
		track.setCurrent(track.get(i));
	}
	
	
	@Override
	public void teardown() {
		t.getDrum1().getCycle().setCustom(null);
		Cycle.setTrigger(false);
		Cycle.setVerse(false);
	}
	
	
}
