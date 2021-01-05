package net.judah.looper;

import java.util.Arrays;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.JudahZone;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeNotifier;
import net.judah.mixer.Channel;

public class DrumTrack extends Recorder implements TimeListener {

	public static final String NAME = "_drums";
	final Channel soloTrack;
	
	public DrumTrack(TimeNotifier master, Channel soloTrack) { 
		super(NAME, Type.SOLO, Arrays.asList(new JackPort[] {
				soloTrack.getLeftPort(), soloTrack.getRightPort()
		}), JudahZone.getOutPorts());
		master.addListener(this);
		soloTrack.setSolo(true);
		this.soloTrack = soloTrack;
	}

	@Override
	public void update(Property prop, Object value) {
		if (Property.STATUS == prop) {
			if (Status.ACTIVE == value) 
				record(true);
			if (Status.TERMINATED == value) {
				record(false);
				play(true); // armed
				soloTrack.setOnMute(true);
				soloTrack.setMuteRecord(true);
				soloTrack.getGui().update();
			}
		}
	}

}
