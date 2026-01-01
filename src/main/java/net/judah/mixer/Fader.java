package net.judah.mixer;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.sampler.Sample;

@RequiredArgsConstructor
public class Fader {

	public static final int DEFAULT_FADE = 3000;

	final Object root;
	final Gain gain;
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
				MixWidget fader = JudahZone.getInstance().getMixer().getFader(ch);
				if (fader != null)
					fader.updateVolume();
			};
		return result;
	}

	public Fader(Object master, int msec, int startVal, int endVal) {
		root = master;
		this.msec = msec;
		this.startVal = startVal;
		this.endVal = endVal;
		if (root instanceof Channel ch)
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
		return fadeIn(JudahZone.getInstance().getMains());
	}

	public static Fader fadeOut(Object o) {
		int startVol = 50;
		Runnable cleanup = null;
		if (o instanceof Channel ch) {
			cleanup = () -> JudahZone.getInstance().getMixer().getFader(ch).updateVolume();
			startVol = ch.getVolume();
		}
		else if (o instanceof Sample s) {
			startVol = s.getGain().get(Gain.VOLUME);
			cleanup = () -> {
				s.play(false);
				MainFrame.update(s);
			};
		}
		return new Fader(o, DEFAULT_FADE, startVol, 0).setCleanup(cleanup);
	}

	/** Fade out Master track over 4 seconds */
	public static Fader fadeOut() {
		return fadeOut(JudahZone.getInstance().getMains());
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
