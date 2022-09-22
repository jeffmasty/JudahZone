package net.judah.songs;

import static net.judah.JudahZone.*;

import java.io.File;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.looper.Looper;
import net.judah.midi.ProgChange;
import net.judah.mixer.Channels;
import net.judah.tracker.Track;
import net.judah.tracker.JudahBeatz;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class LoopStory extends SmashHit {

	int count;
	
	
	TimeListener gtrEfx = new TimeListener() {
		@Override public void update(Property prop, Object value) {
			if (value != Status.OVERDUBBED)
				return;
			loops.getLoopC().duplicate();
			ch.getGuitar().setPreset(getPresets().byName("Freedist"));
			loops.getLoopB().removeListener(this);
			ch.getGuitar().setPresetActive(true);
		}
	};
	TimeListener strings = new TimeListener() {
		@Override public void update(Property prop, Object value) {
			if (value != Status.OVERDUBBED) // overdubbed  
				return;
			ProgChange.progChange(44, JudahZone.getMidi().getFluidOut(), 0);
			loops.getDrumTrack().removeListener(this);
			RTLogger.log(this, "OCT!strings, bridge, fade");
		}
	};
	
	@Override
	public void startup(JudahBeatz t, Looper loops, Channels ch, MainFrame frame) {
		super.startup(t, loops, ch, frame);
		frame.sheetMusic(new File(Constants.SHEETMUSIC, "LoveStory.png"));
		count = 0;
		ProgChange.progChange("Acoustic Bass", JudahZone.getMidi().getFluidOut(), 0);
		t.getDrum1().setFile("tinyDrums");
		t.getDrum2().setActive(false);
		t.getDrum3().setActive(false);
		getClock().setLength(8);
		ch.getGuitar().setPreset(getPresets().byName("Freelay"));
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
