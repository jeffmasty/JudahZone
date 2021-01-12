package net.judah.mixer.bus;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.mixer.Channel;
import net.judah.mixer.bus.LFO.Target;

@RequiredArgsConstructor
public class Fader {

	final Channel channel;
	final LFO.Target target;
	final long msec;
	final double startVal, endVal;
	long startTime;

	/** 2 second fade-in on master bus */
	public static Fader fadeIn() {
		return new Fader(JudahZone.getMasterTrack(), Target.Gain, 2000, 0, 100);
	}

	// /** you set the fade-out time on the master gain */
	// public static Fader fadeOut(long msec) { return new Fader(JudahZone.getMasterTrack(), Target.Gain, 4000, 100, 0); }
	
	private static ArrayList<Fader> faders = new ArrayList<>();
	
	private void run(long currentTimeMillis) {
		// query value
		double ratio = (currentTimeMillis - startTime) / (double)msec;
		double val = ratio * (startVal - endVal);
		if (startVal > endVal)
			val = endVal - Math.abs(val);
		
		// set target value on channel
		switch (target) {
			case Gain:  channel.setVolume((int)val); break;  
			case CutEQ: channel.getCutFilter().setFrequency(CutFilter.knobToFrequency((int)val)); break;
			case Reverb: channel.getReverb().setRoomSize((float) val/100f); break;
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
		for (Fader f : new ArrayList<Fader>(faders)) {
			if (current > f.startTime + f.msec) {
				faders.remove(f);
				continue;
			}
			f.run(current);
		}
		
	}
	
}
