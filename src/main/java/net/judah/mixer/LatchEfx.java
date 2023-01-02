package net.judah.mixer;

import java.util.ArrayList;

import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.looper.Loop;
import net.judah.util.RTLogger;


public class LatchEfx implements TimeListener {

	private final ArrayList<Loop> listenOn = new ArrayList<>();
	private final Channel channel;

	public LatchEfx(LineIn line) {
		this.channel = line;
	}

	public void clear() {
		JudahZone.getLooper().removeListener(this);
//		for (Loop a : listenOn.toArray(new Loop[listenOn.size()])) {
//			a.removeListener(this);
//		}
		listenOn.clear();
		RTLogger.log(this, channel.getName() + " FX cleared");
	}
	
	public void latch(Loop... x) {
		clear();
		JudahZone.getLooper().addListener(this);
		
		RTLogger.log(this, channel.getName() + " " + channel.getPreset().getName() + " waiting on looper");
	}
	
	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.LOOP && value == Status.NEW) {
			channel.setPresetActive(!channel.isPresetActive());
			clear();
		}
		else if (prop == Property.LOOP) {
			channel.setPresetActive(!channel.isPresetActive());
			clear();
		}
	}
		
}