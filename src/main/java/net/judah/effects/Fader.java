package net.judah.effects;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.effects.LFO.Target;
import net.judah.effects.api.Gain;
import net.judah.looper.sampler.Sample;
import net.judah.mixer.Channel;

@RequiredArgsConstructor
public class Fader {

	public static final int DEFAULT_FADE = 3000;
	
	final Channel channel;
	final LFO.Target target;
	final long msec;
	final double startVal, endVal;
	long startTime;
	Runnable cleanup;

	public Fader setCleanup(Runnable r) {
		this.cleanup = r;
		return this;
	}
	
	public static Fader fadeIn(Channel ch) {
		return new Fader(ch, Target.Gain, DEFAULT_FADE, 0, ch instanceof Sample ? 95 : 51, new Runnable() {
		    @Override public void run() {
		        ch.getFader().updateVolume();
		    }});
	}
	
	/** 4 second fade-in on master bus */
	public static Fader fadeIn() {
		return fadeIn(JudahZone.getMains());
	}

	
	public static Fader fadeOut(Channel ch) {
	    Fader result =  new Fader(ch, Target.Gain, DEFAULT_FADE, ch.getVolume(), 0, new Runnable() {
            @Override public void run() {
            	ch.getFader().updateVolume();
            }
        });
	    if (ch instanceof Sample)
			result.cleanup = () ->{
				((Sample)ch).setActive(false);
				((Sample)ch).getPad().update();};
		return result;
	}
	/** Fade out Master track over 4 seconds */
	public static Fader fadeOut() {
		return fadeOut(JudahZone.getMains());
	}

    public Fader(Channel master, Target gain, int msec, int startVal, int endVal, Runnable cleanup) {
        this(master, gain, msec, startVal, endVal);
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
		switch (target) {
			case Gain:  channel.getGain().setVol((int)val); break;
			case CutEQ: channel.getCutFilter().setFrequency(CutFilter.knobToFrequency((int)val)); break;
			case Reverb: channel.getReverb().setRoomSize((float) val * 0.01f); break;
			case Delay: channel.getDelay().setFeedback((float) val * 0.01f); break;
			case Pan: channel.getGain().set(Gain.PAN, (int)val); break;
		}
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
