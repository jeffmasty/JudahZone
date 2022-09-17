package net.judah.songs;

import java.io.File;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.ProgChange;
import net.judah.mixer.Channels;
import net.judah.tracker.Track;
import net.judah.tracker.Tracker;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class LoopStory extends SmashHit {

	int count;
	
	
	TimeListener gtrEfx = new TimeListener() {
		@Override public void update(Property prop, Object value) {
			if (value != Status.OVERDUBBED)
				return;
			loops.getLoopC().duplicate();
			ch.getGuitar().setPreset(JudahZone.getPresets().byName("Freedist"));
			loops.getLoopB().removeListener(this);
			ch.getGuitar().setPresetActive(true);
		}
	};
	TimeListener strings = new TimeListener() {
		@Override public void update(Property prop, Object value) {
			if (value != Status.OVERDUBBED) // overdubbed  
				return;
			ProgChange.progChange(44, JudahMidi.getInstance().getFluidOut(), 0);
			loops.getDrumTrack().removeListener(this);
			RTLogger.log(this, "OCT!strings, bridge, fade");
		}
	};
	
	@Override
	public void startup(Tracker t, Looper loops, Channels ch) {
		super.startup(t, loops, ch);
		MainFrame.get().sheetMusic(new File(Constants.SHEETMUSIC, "LoveStory.png"));
		count = 0;
		ProgChange.progChange("Acoustic Bass", JudahMidi.getInstance().getFluidOut(), 0);
		t.getDrum1().setFile("tinyDrums");
		t.getDrum2().setActive(false);
		t.getDrum3().setActive(false);
		JudahClock.getInstance().setLength(8);
		ch.getGuitar().setPreset(JudahZone.getPresets().byName("Freelay"));
		ch.getGuitar().setPresetActive(true);
		ch.getGuitar().getLatchEfx().latch(loops.getLoopA());
		
		t.getDrum1().getCycle().setCustom(this);
		t.getDrum1().setActive(true);
		loops.getLoopB().setArmed(true);
		loops.getDrumTrack().addListener(strings);
		loops.getLoopB().addListener(gtrEfx);
		MainFrame.update(loops.getLoopB());
		loops.getLoopC().setOnMute(true);
		MainFrame.update(loops.getLoopC());
		if (!t.getClock().isActive())
			t.getClock().begin();
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
		t.getDrum1().getCycle().setCustom(null);
		loops.getLoopB().removeListener(gtrEfx);
		loops.getDrumTrack().removeListener(strings);
	}
	
}
