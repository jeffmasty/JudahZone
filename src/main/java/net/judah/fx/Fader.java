package net.judah.fx;

import static net.judah.JudahZone.getMixer;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.mixer.FxChain;
import net.judah.sampler.Sample;

@RequiredArgsConstructor
public class Fader {

	public static final int DEFAULT_FADE = 3000;

	final Object root;
	final Gain gain;
	final DJJefe mixer = JudahZone.getMixer();
	final long msec;
	final double startVal, endVal;
	long startTime;
	Runnable cleanup;

	public Fader setCleanup(Runnable r) {
		this.cleanup = r;
		return this;
	}

	public static Fader fadeIn(Object o) {
		Fader result = new Fader(o, DEFAULT_FADE, 0, 51);

		if (o instanceof Channel ch)
			result.cleanup = () -> {
				if (getMixer().getFader(ch) != null)
					getMixer().getFader(ch).updateVolume();
			};
//		if (o instanceof Sample s)
		return result;
	}

	public Fader(Object master, int msec, int startVal, int endVal) {
		root = master;
		this.msec = msec;
		this.startVal = startVal;
		this.endVal = endVal;
		if (root instanceof FxChain ch)
			gain = ch.getGain();
		else if (root instanceof Sample s)
			gain = s.getGain();
		else
			gain = null;
	}

    public Fader(Object master, int msec, int startVal, int endVal, Runnable cleanup) {
        this(master, msec, startVal, endVal);
        this.cleanup = cleanup;
    }

	private static ArrayList<Fader> faders = new ArrayList<>();

	/** 4 second fade-in on master bus */
	public static Fader fadeIn() {
		return fadeIn(JudahZone.getMains());
	}

	public static Fader fadeOut(Object o) {
		int startVol = 50;
		Runnable cleanup = null;
		if (o instanceof Channel ch) {
			cleanup = () -> getMixer().getFader(ch).updateVolume();
			startVol = ch.getVolume();
		}
		else if (o instanceof Sample s) {
			startVol = s.getGain().get(Gain.VOLUME);
			cleanup = () -> s.play(false);
		}
		Fader result = new Fader(o, DEFAULT_FADE, startVol, 0);
		result.cleanup = cleanup;
		return result;
	}
	/** Fade out Master track over 4 seconds */
	public static Fader fadeOut() {
		return fadeOut(JudahZone.getMains());
	}


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
		gain.set(Gain.VOLUME, (int)val);
		MainFrame.update(root);
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
