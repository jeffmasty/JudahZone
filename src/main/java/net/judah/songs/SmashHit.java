package net.judah.songs;

import java.io.File;

import net.judah.JudahZone;
import net.judah.looper.Looper;
import net.judah.mixer.Channel;
import net.judah.mixer.Channels;
import net.judah.tracker.Track;
import net.judah.tracker.Tracker;
import net.judah.util.RTLogger;

public abstract class SmashHit {
	
	protected Tracker t;
	protected Looper loops;
	protected Channels ch;
	
	public void startup(Tracker t, Looper loops, Channels ch) { 
		this.t = t;
		this.loops = loops;
		this.ch = ch;
	}
	
	public void cycle(Track t) { 
		RTLogger.log(this, "Empty cycle() on " + t);
	}
	
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
