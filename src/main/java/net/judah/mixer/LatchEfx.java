package net.judah.mixer;

import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.util.RTLogger;


public class LatchEfx implements TimeListener {

	private final Channel channel;

	public LatchEfx(LineIn line) {
		this.channel = line;
	}

	public void clear() {
		JudahZone.getClock().removeListener(this);
		MainFrame.update(channel);
		RTLogger.log(this, channel.getName() + " FX cleared");
	}
	
	public void latch() {
		JudahZone.getClock().addListener(this);
		MainFrame.update(channel);
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