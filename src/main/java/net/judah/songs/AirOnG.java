package net.judah.songs;

import java.io.File;

import net.judah.JudahZone;
import net.judah.Looper;
import net.judah.MainFrame;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.effects.api.PresetsDB.Raw;
import net.judah.looper.Loop;
import net.judah.mixer.LineIn;
import net.judah.tracker.Track;
import net.judah.tracker.Tracker;

public class AirOnG extends SmashHit {

	Track track;
	
	@Override public void startup(Tracker t) {
		track = t.getChords();
		track.setFile(new File(track.getFolder(), "AirOnG"));
		track.getCycle().setCustom(this);
		Looper looper = JudahZone.getLooper();
		final Loop a = looper.getLoopA();
		final Loop b = looper.getLoopB();
		final Loop c = looper.getLoopC();
		TimeListener muteListener = new TimeListener() {
			@Override public void update(Property prop, Object value) {
				if (prop == Property.LOOP) {
					c.record(true);
					a.removeListener(this);
					a.setOnMute(true);
					b.setOnMute(true);
		}}};
		TimeListener airListener = new TimeListener() {
			@Override public void update(Property prop, Object value) {
				if (prop == Property.LOOP) {
					track.setActive(true);
					a.removeListener(this);
					a.addListener(muteListener);
					c.duplicate(); 
					c.duplicate();
		}}};
		a.addListener(airListener);
		a.setOnMute(false);
		b.setArmed(true);
		b.setOnMute(false);
		track.getClock().setLength(12);
		track.setActive(false); // on listener
		JudahZone.getChannels().getFluid().setMuteRecord(true);
		LineIn mic = JudahZone.getChannels().getMic();
		mic.setMuteRecord(false);
		mic.getGain().setVol(50);
		LineIn gtr = JudahZone.getChannels().getGuitar();
		gtr.setMuteRecord(false);		
		gtr.setPreset(JudahZone.getPresets().get(Raw.Freeverb));
		gtr.getLatchEfx().latch(a);
		gtr.reset();
		MainFrame.update(b);
		MainFrame.updateTime();
		MainFrame.update(gtr);
		MainFrame.get().sheetMusic(new File("/home/judah/sheets/AirOnG.png"));
	}
	
	@Override
	public void cycle(Track t) {
		
	}

	@Override public void teardown() {
	}


}
