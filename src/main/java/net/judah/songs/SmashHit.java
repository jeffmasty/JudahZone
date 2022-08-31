package net.judah.songs;

import java.io.File;

import net.judah.JudahZone;
import net.judah.mixer.Channel;
import net.judah.tracker.Track;
import net.judah.tracker.Tracker;

public abstract class SmashHit {
	
	public void startup(Tracker t) { }
	
	public void cycle(Track t) { }
	
	public void teardown() { }

	@Override
	public final String toString() {
		return this.getClass().getSimpleName();
	}

	/** if different */
	protected void setFile(Track t, File f) {
		if (false == f.equals(t.getFile()))
			t.setFile(f);
	}

	protected void resetChannels() {
		for (Channel ch : JudahZone.getChannels())
			ch.reset();
	}
}
