package net.judah.songs;

import net.judah.JudahZone;
import net.judah.looper.Loop;
import net.judah.tracker.Cycle;
import net.judah.tracker.Track;

public class AllMyLovin extends SmashHit {

	private Loop a;
	
	@Override
	public void startup() {
		super.startup();
		a = looper.getLoopA();
		drum1.setFile("AllMyLovin");
		drum1.setActive(false);
		drum2.setActive(false);
		fills.setActive(false);
		frame.sheetMusic("AllMyLovin.png");
		
		clock.writeTempo(93);
		clock.setLength(10);
		guitar.setPreset(JudahZone.getPresets().byName("Freeverb"));
		guitar.setPresetActive(false);
		guitar.getLatchEfx().latch(looper.getLoopA());
		drum1.getCycle().setCustom(this);
		
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
		drum1.getCycle().setCustom(null);
	}
}
