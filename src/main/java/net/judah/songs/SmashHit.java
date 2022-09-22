package net.judah.songs;

import java.io.File;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.looper.Looper;
import net.judah.mixer.Channel;
import net.judah.mixer.Channels;
import net.judah.tracker.Track;
import net.judah.tracker.JudahBeatz;
import net.judah.util.RTLogger;

public abstract class SmashHit {
	
	protected JudahBeatz t;
	protected Looper loops;
	protected Channels ch;
	protected MainFrame frame;
	
	public void startup(JudahBeatz t, Looper loops, Channels ch, MainFrame frame) { 
		this.t = t;
		this.loops = loops;
		this.ch = ch;
		this.frame = frame;
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
		for (Channel ch : JudahZone.getMixer().getChannels())
			ch.reset();
	}
}
