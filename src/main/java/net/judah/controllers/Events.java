package net.judah.controllers;

import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.AudioMode;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.effects.api.Effect;
import net.judah.looper.Loop;
import net.judah.midi.JudahClock;
import net.judah.util.RTLogger;

public class Events {

	@RequiredArgsConstructor
	public static class Record implements Runnable {
		private final Loop target;
		
		@Override public void run() {
			target.record(target.isRecording() != AudioMode.RUNNING);		
		}
	}

	@RequiredArgsConstructor
	public static class LatchEfx /* implements Runnable */ {
		private final Loop target;
		private final Effect reverb;
		
		public LatchEfx() {
			this.reverb = JudahZone.getChannels().getGuitar().getReverb();
			this.target = JudahZone.getLooper().getLoopA();
			target.addListener(new TimeListener() {
				@Override public void update(Property prop, Object value) {
					if (Property.STATUS == prop && Status.TERMINATED == value) {
						reverb.setActive(!reverb.isActive());
						target.removeListener(this);
						MainFrame.update(JudahZone.getChannels().getGuitar());
					}
				}
			});
			RTLogger.log(this, reverb.getName() + " armed.");
		}
//		@Override public void run() {
//			target.addListener(new TimeListener() {
//				@Override public void update(Property prop, Object value) {
//					if (Property.STATUS == prop && Status.TERMINATED == value) {
//						reverb.setActive(!reverb.isActive());
//						target.removeListener(this);
//						MainFrame.update(JudahZone.getChannels().getGuitar());
//					}
//				}
//			});
//			RTLogger.log(this, reverb.getName() + " armed.");
//		}
		
	}
	
	
	public static class ClockMode implements Runnable {
		@Override public void run() {
			JudahClock.getInstance().toggleMode();
		}
	}
	
}
