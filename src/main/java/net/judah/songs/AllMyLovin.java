package net.judah.songs;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.mixer.Channels;
import net.judah.mixer.Instrument;
import net.judah.tracker.Cycle;
import net.judah.tracker.Track;
import net.judah.tracker.Tracker;

public class AllMyLovin extends SmashHit {

	private Track drum;
	private Loop a;
	
	@Override
	public void startup(Tracker t, Looper loops, Channels ch) {
		super.startup(t, loops, ch);
		a = loops.getLoopA();
		drum = t.getDrum1();
		drum.setFile("AllMyLovin");
		drum.setActive(false);
		t.getDrum2().setActive(false);
		t.getDrum3().setActive(false);
		MainFrame.get().sheetMusic("AllMyLovin.png");
		
		t.getClock().writeTempo(93);
		t.getClock().setLength(10);
		Instrument gtr = ch.getGuitar();
		gtr.setPreset(JudahZone.getPresets().byName("Freeverb"));
		gtr.setPresetActive(false);
		gtr.getLatchEfx().latch(loops.getLoopA());
		drum.getCycle().setCustom(this);
		
		// marimba thing
		
	}

	
	@Override
	public void cycle(Track t) {
		if (Cycle.isVerse()) {
			t.setCurrent(t.get(1));
			a.setOnMute(true);
		}
		else {
			if (Cycle.isTrigger()) {
				Cycle.setTrigger(false);
				a.setTapeCounter(0);
				a.setOnMute(false);
			}
			t.setCurrent(t.get(0));
			
		}
	}
	
	@Override
	public void teardown() {
		drum.getCycle().setCustom(null);
	}
}
