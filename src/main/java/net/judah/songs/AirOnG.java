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
import net.judah.util.Constants;

public class AirOnG extends SmashHit {

	

	@Override
	public void startup() {
		super.startup();
		chords.setFile("AirOnG");
		chords.getClock().setLength(12);
		chords.getClock().writeTempo(90);
		chords.setActive(false); // on listener
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
					chords.setActive(true);
					a.removeListener(this);
					a.addListener(muteListener);
		}}};
		a.addListener(airListener);
		a.setOnMute(false);
		b.setArmed(true);
		b.setOnMute(false);
		fluid.setMuteRecord(true);
		mic.setMuteRecord(false);
		mic.getGain().setVol(50);
		guitar.setMuteRecord(false);		
		guitar.setPreset(JudahZone.getPresets().get(Raw.Freeverb));
		guitar.getLatchEfx().latch(a);
		guitar.reset();
		MainFrame.update(b);
		MainFrame.updateTime();
		MainFrame.update(guitar);
		frame.sheetMusic(new File("/home/judah/sheets/AirOnG.png"));
	}
	


}
