package net.judah.songs;

import net.judah.MainFrame;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.effects.Delay;
import net.judah.looper.Looper;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channels;
import net.judah.mixer.Instrument;
import net.judah.tracker.Cycle;
import net.judah.tracker.Track;
import net.judah.tracker.Tracker;
import net.judah.util.Constants;

public class FeelLuv extends SmashHit {

	private final int TOTAL_LEN = 40;
	private final int BPM = 120;
	Track bass;
	Track hats;
	Track kick;
	Track clap;
	Track chords;
	Instrument fluid;
	Instrument crave;

	private int count;
	private boolean vamp;

	private TimeListener init = new TimeListener() {
		@Override public void update(Property prop, Object value) {
			if (value != Status.ACTIVE)
				return;
			
			vamp = false;
			hats.setActive(true);
			loops.getLoopA().removeListener(this);
			
		}
	};
	
	@Override
	public void startup(Tracker t, Looper loops, Channels ch) {
		super.startup(t, loops, ch);
		MainFrame.get().sheetMusic("FeelLuv.png");
		t.getClock().writeTempo(BPM);
		t.getClock().end();
		t.getClock().setLength(TOTAL_LEN);
		loops.getLoopA().addListener(init);
		loops.getLoopB().setArmed(true);
		
		JudahMidi.getInstance().getGui().getMpk().setSelectedItem(JudahMidi.getInstance().getCraveOut());
		MainFrame.updateTime();
		
		bass = t.getBass();
		hats = t.getDrum2();
		kick = t.getDrum1();
		clap = t.getDrum3();
		chords = t.getChords();
		fluid = ch.getFluid();
		crave = ch.getCrave();
		
		kick.setActive(false);
		clap.setActive(false);
		hats.setActive(false);
		
		kick.setFile("FeelKick");
		clap.setFile("FeelClap");
		bass.setFile("FeelLove");
		bass.getCycle().setCustom(this);
		
		fluid.setPreset("FeelLuv");
		Delay delay = fluid.getDelay();
		delay.setActive(false);
		// slapback delay BPM/2 in seconds
		delay.setDelay(Constants.millisPerBeat(BPM)*0.0005f); 
		delay.setSlapback(true);
		fluid.setMuteRecord(true);
		fluid.setPresetActive(true);
		hats.setActive(false);
		hats.setPattern("closed");
		fluid.getGain().setPan(66);
		fluid.getGain().setVol(0);
		
		chords.setFile("FeelFLute");
		
		crave.getReverb().setActive(true);
		count = 0;
		vamp = true;
		
		ch.getMic().setMuteRecord(false);
		MainFrame.update(crave);
		MainFrame.update(fluid);
		kick.setActive(true);
		bass.setActive(true);
		t.getClock().begin();
		
	}

	@Override
	public void cycle(Track t) {
		if (t == bass)
			bassCycle();
	}

	private void bassCycle() {
		if (Cycle.isTrigger()) { 
			Cycle.setTrigger(false);
			bass.setPattern(("C"));
			count = 0;
			vamp = !vamp;
		}

		if (vamp)
			return;
		
		count++;
		switch(count) {
			case 8: bass.setPattern("Eb"); return;
			case 12: bass.setPattern("F"); return;
			case 16: bass.setPattern("G"); return;
			case 24:
			case 28:
				bass.setPattern("C"); return;
			case 25: 
			case 29:
				bass.setPattern("Eb"); return;
			case 26: 
			case 30:
				bass.setPattern("F"); return;
			case 27: 
			case 31:
				bass.setPattern("G"); return;
			case 32: bass.setPattern("C"); return;
			case TOTAL_LEN: count = 0;
		}
		
	}
	
	@Override
	public void teardown() {
		fluid.getDelay().setSlapback(false);
		bass.getCycle().setCustom(null);
	}
	
}
