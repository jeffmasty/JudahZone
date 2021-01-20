package net.judah.effects;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.effects.LFO.Target;
import net.judah.effects.gui.EffectsRack;
import net.judah.mixer.Channel;
import net.judah.mixer.MasterTrack;

@RequiredArgsConstructor
public class Fader {

	final Channel channel;
	final LFO.Target target;
	final long msec;
	final double startVal, endVal;
	long startTime;
	Runnable cleanup;

	/** 3 second fade-in on master bus */
	public static Fader fadeIn() {
		return new Fader(JudahZone.getMasterTrack(), Target.Gain, 3000, 0, 51, new Runnable() {
		    @Override public void run() {
		        EffectsRack.volume(JudahZone.getMasterTrack());
		        JudahZone.getMasterTrack().getGui().update();
		    }
		});
	}

	/** Fade out Master track over 4 seconds */
	public static Fader fadeOut() {
	    final MasterTrack master = JudahZone.getMasterTrack();
	    return new Fader(master, Target.Gain, 4000, master.getVolume(), 0, new Runnable() {
            @Override
            public void run() {
                master.setOnMute(true);
                master.setVolume(51);
            }
        });
	}

    public Fader(MasterTrack master, Target gain, int msec, int startVal, int endVal, Runnable cleanup) {
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
			case Gain:  channel.setVolume((int)val); break;
			case CutEQ: channel.getCutFilter().setFrequency(CutFilter.knobToFrequency((int)val)); break;
			case Reverb: channel.getReverb().setRoomSize((float) val/100f); break;
			case Delay: channel.getDelay().setFeedback((float) val/100f); break;
			case Pan: channel.setPan((float) val/100f); break;
		}
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
