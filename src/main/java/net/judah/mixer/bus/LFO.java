package net.judah.mixer.bus;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.JudahZone;
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;

/** A calculated sin wave LFO.  Default amplitude returns queries between 0 and 100 */
@Data @NoArgsConstructor @AllArgsConstructor
public class LFO {
	
	public static enum Target{
		Gain, CutEQ, Reverb, Delay
	};
	
	private boolean active;
	private Target target = Target.Gain;
	
	/** in kilohertz (msec per cycle). default: 1000 (1 per second)*/
	private double frequency = 666;
	
	/** align the wave on the jack frame buffer by millisecond */
	private long shift;
	
	/** set output level of queries. default: 0 to 100. */
	private double amplitude = 100;
	
	private static final ArrayList<Channel> lfoPulse = new ArrayList<>();
	
	/** called every MidiScheduler.LFO_PULSE jack frames in RT thread */
	public static void pulse() {
		for (LineIn ch : JudahZone.getChannels())
			if (ch.getLfo().isActive())
				lfoPulse.add(ch);
		for (Sample s : JudahZone.getLooper()) 
			if (s.getLfo().isActive())
				lfoPulse.add(s);
		if (lfoPulse.isEmpty()) return;
		new Thread() { @Override public void run() {
			for (Channel ch : lfoPulse) 
				ch.getLfo().pulse(ch);
			lfoPulse.clear();
		}}.start();
	}

	/** query the oscillator at a specific time, System.currentTimeMillis(), for example. */
	public double query(long millis) {
		// divide current millis + offset by frequency and apply sin wave, multiply by amplitude
		long time = millis + shift;
		double phase = (time % frequency) / frequency;
		double wave = 1 + Math.sin(Math.toRadians(phase * 360));
		return (wave * (amplitude / 2));
	}
	
	/** query for right now. */
	public double query() {
		return query(System.currentTimeMillis());
	}
	
	/** execute the LFO on the channel's target */
	public void pulse(Channel ch) {
		int val = (int)query();
		switch(target) {
			case Gain: ch.setVolume(val); break;
			case CutEQ: ch.getCutFilter().setFrequency(
					CutFilter.knobToFrequency((int)ch.getLfo().query())); break;
			case Reverb: ch.getReverb().setRoomSize(val / 100f);  
						 ch.getReverb().setWidth(val / 100f); 
						 ch.getReverb().setDamp(val / 100f); break;
			case Delay: ch.getDelay().setFeedback(val / 100f);
		}
	}

	// TODO saw wave
	//private final static float TWOPI = (float) (2 * Math.PI);
	//public double querySaw() {
	//	long time = System.currentTimeMillis() + shift;
	//	double phase = (time % frequency) / frequency;
	//    double t = phase / TWOPI;
	//    return (2.0f * t) - 1.0f;
	//}

	public static class LFOTest extends Thread {
		final LFO lfo = new LFO();

		long sleep = 100;
		final long spells = 150;
		int count;

		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		
		public LFOTest(int freq) {
			lfo.setFrequency(freq); 
		}
		
		@Override
		public void run() {
			while(count <= spells) {
				double query = lfo.query();
				if (query > max) max = query;
				if (query < min) min = query;

				long spaces = Math.round(query / 5);
				String display = "";
				for (int i = 0; i< spaces; i++)
					display += " ";
				display += "*";
				System.out.println(display + "       " + query);
				
				count++;
				try { Thread.sleep(sleep);
				} catch (Throwable t) { }
			}
			System.out.println("done. min: " + min + " max: " + max);
		}
		public static void main(String[] args) {
			new LFOTest(3000).start(); // 3 second LFO
		}
	}

}