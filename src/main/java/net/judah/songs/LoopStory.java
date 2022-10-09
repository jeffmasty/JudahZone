package net.judah.songs;

import static net.judah.JudahZone.*;

import java.io.File;

import net.judah.MainFrame;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.midi.ProgChange;
import net.judah.tracker.Track;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class LoopStory extends SmashHit {

	int count;
	
	
	TimeListener gtrEfx = new TimeListener() {
		@Override public void update(Property prop, Object value) {
			if (value != Status.OVERDUBBED)
				return;
			looper.getLoopC().duplicate();
			guitar.setPreset(getPresets().byName("Freedist"));
			looper.getLoopB().removeListener(this);
			guitar.setPresetActive(true);
		}
	};
	TimeListener strings = new TimeListener() {
		@Override public void update(Property prop, Object value) {
			if (value != Status.OVERDUBBED) // overdubbed  
				return;
			ProgChange.progChange(44, fluid, 0);
			looper.getDrumTrack().removeListener(this);
			RTLogger.log(this, "OCT!strings, bridge, fade");
		}
	};
	
	@Override
	public void startup() {
		super.startup();
		frame.sheetMusic(new File(Constants.SHEETMUSIC, "LoveStory.png"));
		count = 0;
		ProgChange.progChange("Acoustic Bass", fluid, 0); // TODO
		drum1.setFile("tinyDrums");
		drum2.setActive(false);
		fills.setActive(false);
		getClock().setLength(8);
		guitar.setPreset(getPresets().byName("Freelay"));
		guitar.setPresetActive(true);
		guitar.getLatchEfx().latch(looper.getLoopA());
		
		drum1.getCycle().setCustom(this);
		drum1.setActive(true);
		looper.getLoopB().setArmed(true);
		looper.getDrumTrack().addListener(strings);
		looper.getLoopB().addListener(gtrEfx);
		MainFrame.update(looper.getLoopB());
		looper.getLoopC().setOnMute(true);
		MainFrame.update(looper.getLoopC());
		if (!clock.isActive())
			clock.begin();
		RTLogger.log(this, "lead,chords, OCT!bass, C-RECverse");
	}

	@Override
	public void cycle(Track t) {
		count++;
		if (count == 1)
			t.next(true);
		else 
			t.next(count % 2 == 0);
	}
	
	
	@Override
	public void teardown() {
		drum1.getCycle().setCustom(null);
		looper.getLoopB().removeListener(gtrEfx);
		looper.getDrumTrack().removeListener(strings);
	}
	
}
