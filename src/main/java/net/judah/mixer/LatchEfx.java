package net.judah.mixer;

import java.util.ArrayList;

import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.looper.Loop;


public class LatchEfx implements TimeListener {

	private final ArrayList<Loop> listenOn = new ArrayList<>();
	private final Channel channel;

	public LatchEfx(LineIn line) {
		this.channel = line;
	}

	public void clear() {
		for (Loop a : listenOn) {
			a.removeListener(this);
		}
		listenOn.clear();
	}
	
	public void latch(Loop... x) {
		clear();
		for (Loop a : x) {
			listenOn.add(a);
			a.addListener(this);
		}
	}
	
	@Override
	public void update(Property prop, Object value) {
		if (Property.STATUS == prop && Status.TERMINATED == value) {
			channel.setPresetActive(!channel.isPresetActive());
			clear();
		}
	}
		
}