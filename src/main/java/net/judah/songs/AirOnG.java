package net.judah.songs;

import java.io.File;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.effects.Fader;
import net.judah.effects.LFO.Target;
import net.judah.effects.api.PresetsDB.Raw;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.mixer.LineIn;
import net.judah.settings.Channels;
import net.judah.tracker.Track;
import net.judah.tracker.Tracker;
import net.judah.util.Constants;

public class AirOnG extends SmashHit {

	Track track;
	
	@Override public void startup(Tracker t, Looper looper, Channels ch) {
		track = t.getChords();
		track.setFile(new File(track.getFolder(), "AirOnG"));
		track.getClock().setLength(12);
		track.setActive(false); // on listener
		// track.getCycle().setCustom(this);  // ABCD..

		final Loop a = looper.getLoopA();
		final Loop b = looper.getLoopB();
		final Loop c = looper.getLoopC();
		TimeListener muteListener2 = new TimeListener() {
			@Override public void update(Property prop, Object value) {
				if (prop == Property.LOOP) {
					c.record(true);
					b.removeListener(this);
					b.setOnMute(true);
				}
			}
		};
		TimeListener muteListener = new TimeListener() {
			@Override public void update(Property prop, Object value) {
				if (prop == Property.LOOP) {
					a.removeListener(this);
					c.duplicate(); 
					Constants.timer(100, ()-> {
						c.duplicate();
						b.addListener(muteListener2);	
					});
					Constants.timer(1000, ()->
						Fader.execute(new Fader(a, Target.Gain, Fader.DEFAULT_FADE, a.getVolume(), 0)));
					a.setOnMute(true);
				}}};
		TimeListener airListener = new TimeListener() {
			@Override public void update(Property prop, Object value) {
				if (prop == Property.LOOP) {
					track.setActive(true);
					a.removeListener(this);
					a.addListener(muteListener);
		}}};
		a.addListener(airListener);
		a.setOnMute(false);
		b.setArmed(true);
		b.setOnMute(false);
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
	


}
