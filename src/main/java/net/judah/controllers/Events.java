package net.judah.controllers;

import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.ControlPanel;
import net.judah.api.AudioMode;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.clock.JudahClock;
import net.judah.effects.api.Effect;
import net.judah.looper.Recorder;
import net.judah.util.RTLogger;

public class Events {

	@RequiredArgsConstructor
	public static class Record implements Runnable {
		private final Recorder target;
		public Record() {
			target = ControlPanel.getLiveLoop();
		}
		
		@Override public void run() {
			target.record(target.isRecording() != AudioMode.RUNNING);		
		}
	}

	@RequiredArgsConstructor
	public static class LatchEfx implements Runnable {
		private final Effect reverb;
		public LatchEfx() {
			this.reverb = JudahZone.getChannels().getGuitar().getReverb();
		}
		@Override public void run() {
			Recorder target = ControlPanel.getLiveLoop();
			target.addListener(new TimeListener() {
				@Override public void update(Property prop, Object value) {
					if (Property.STATUS == prop && Status.TERMINATED == value) {
						reverb.setActive(!reverb.isActive());
						target.removeListener(this);
					}
				}
			});
			RTLogger.log(this, reverb.getName() + " armed.");
		}
		
	}
	
	
	public static class ClockMode implements Runnable {
		@Override public void run() {
			JudahClock.getInstance().toggleMode();
		}
	}
	
}
