package net.judah.mixer;

import java.util.ArrayList;

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
		for (Loop a : listenOn.toArray(new Loop[listenOn.size()])) {
			a.removeListener(this);
		}
		listenOn.clear();
		RTLogger.log(this, channel.getName() + " Efx cleared");
	}
	
	public void latch(Loop... x) {
		clear();
		for (Loop a : x) {
			listenOn.add(a);
			a.addListener(this);
		}
		RTLogger.log(this, channel.getName() + " Efx waiting on looper");
	}
	
	@Override
	public void update(Property prop, Object value) {
		if (Property.STATUS == prop && Status.TERMINATED == value) {
			channel.setPresetActive(!channel.isPresetActive());
			clear();
		}
		else if (Property.LOOP == prop) {
			channel.setPresetActive(!channel.isPresetActive());
			clear();
		}
	}
		
}