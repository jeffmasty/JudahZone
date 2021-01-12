package net.judah.mixer;

import java.util.Arrays;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeNotifier;
import net.judah.looper.Recorder;
import net.judah.looper.Sample;
import net.judah.plugin.BeatBuddy;
import net.judah.util.Console;

@Data @EqualsAndHashCode(callSuper = true) @Log4j
public class DrumTrack extends Recorder implements TimeListener {

	public static final String NAME = "_drums";
	final LineIn soloTrack;
	TimeNotifier master;
	
	public DrumTrack(TimeNotifier master, LineIn soloTrack) { 
		super(NAME, Type.SOLO, Arrays.asList(new JackPort[] {
				soloTrack.getLeftPort(), soloTrack.getRightPort()
		}), JudahZone.getOutPorts());
		this.master = master;
		master.addListener(this);
		soloTrack.setSolo(true);
		this.soloTrack = soloTrack;
	}

	@Override
	public void update(Property prop, Object value) {
		if (Property.STATUS == prop) {
			
			log.info(prop + ": " + value);
			
			if (Status.ACTIVE == value) 
				record(true);
			if (Status.TERMINATED == value) {
				JudahZone.getDrummachine().setQueue(BeatBuddy.PAUSE_MIDI);
				record(false);
				// play(true); // armed
				new Thread() { // concurrent modification
					@Override public void run() {
						try {Thread.sleep(1); 
						} catch (Throwable t) { }
						master.removeListener(DrumTrack.this);
					};
				}.start();
				soloTrack.setSolo(false);

			}
		}
	}

	public void toggle(boolean engage) {
		if (engage) {
			Sample s = JudahZone.getLooper().get(0); 
			s.addListener(this);
			master = s;
			soloTrack.setSolo(true);
			play(true); // armed
			Console.info("drumtrack sync'd.");
		}
		else {
			if (master != null) 
				master.removeListener(this);
			master = null;
			soloTrack.setSolo(false);
			Console.info("drumtrack disengaged.");
		}
	}
	
	public void toggle() {
		if (soloTrack.isSolo()) // disengage drumtrack
			toggle(false);
		else { // engage drumtrack
			toggle(true);
		}
		if (gui != null)
			gui.update();
	}
	
}
