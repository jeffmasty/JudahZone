package net.judah.fx;

import static net.judah.JudahZone.getMixer;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.drumkit.Sample;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;

@RequiredArgsConstructor
public class Fader {

	public static final int DEFAULT_FADE = 3000;
	
	final Channel channel;
	final DJJefe mixer = JudahZone.getMixer();
	final long msec;
	final double startVal, endVal;
	long startTime;
	Runnable cleanup;

	public Fader setCleanup(Runnable r) {
		this.cleanup = r;
		return this;
	}
	
	public static Fader fadeIn(Channel ch) {
		return new Fader(ch, DEFAULT_FADE, 0, 51, new Runnable() {
		    @Override public void run() {
		    	if (getMixer().getFader(ch) != null)
		    		getMixer().getFader(ch).updateVolume();
		    }});
	}
	
	/** 4 second fade-in on master bus */
	public static Fader fadeIn() {
		return fadeIn(JudahZone.getMains());
	}
	
	public static Fader fadeOut(Channel ch) {
	    Fader result =  new Fader(ch, DEFAULT_FADE, ch.getVolume(), 0, new Runnable() {
            @Override public void run() {
            	if (getMixer().getFader(ch) != null)
            		getMixer().getFader(ch).updateVolume();
            }
        });
	    if (ch instanceof Sample)
			result.cleanup = () ->{
				((Sample)ch).play(false);
				MainFrame.update(ch);};
		return result;
	}
	/** Fade out Master track over 4 seconds */
	public static Fader fadeOut() {
		return fadeOut(JudahZone.getMains());
	}

    public Fader(Channel master, int msec, int startVal, int endVal, Runnable cleanup) {
        this(master, msec, startVal, endVal);
        this.cleanup = cleanup;
    }

	private static ArrayList<Fader> faders = new ArrayList<>();

	private void run(long currentTimeMillis) {
	    boolean up = true;
	    if (startVal > endVal)
	        up = false;

		// query value
		float ratio = (currentTimeMillis - startTime) / (float)msec;
		double val;
		if (up)
		    val = ratio * (endVal - startVal);
		else
		    val = ratio * (endVal - startVal) + (startVal - endVal);

		// set target value on channel
		channel.getGain().set(Gain.VOLUME, (int)val); 
		MainFrame.update(channel);
	}

	public static void execute(Fader f) {
		f.startTime = System.currentTimeMillis();
		f.run(f.startTime);
		faders.add(f);
	}

	private static long current;
	
	public static void pulse() {
		if (faders.isEmpty()) return;
		current = System.currentTimeMillis();
		for (Fader f : new ArrayList<>(faders)) {
			if (current > f.startTime + f.msec) {
				faders.remove(f);
				if (f.cleanup != null)
				    f.cleanup.run();
			}
			else
			    f.run(current);
		}
	}

}
