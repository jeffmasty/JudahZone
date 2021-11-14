package net.judah.effects;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.JudahZone;
import net.judah.effects.api.Effect;
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;

/** A calculated sin wave LFO.  Default amplitude returns queries between 0 and 85 */
@Data @NoArgsConstructor @AllArgsConstructor
public class LFO implements Effect {

    public enum Settings {
        Target, Min, Max, MSec
    }

	public enum Target{
		CutEQ, Gain, Reverb, Delay, Pan
	};

	private boolean active;
	private Target target = Target.CutEQ;

	/** in kilohertz (msec per cycle). default: oscillates over a 1.2 seconds. */
	private double frequency = 1200;

	public float getFrequency() {
	    return (float)frequency;
	}

	/** align the wave on the jack frame buffer by millisecond (not implemented)*/
	private long shift;

	/** set maximum output level of queries. default: 85. */
	private float max = 85;
	/** set minimum level of queries. default 0. */
	private float min = 0;

    private static final ArrayList<Channel> lfoPulse = new ArrayList<>();

	public void setMax(float val) {
	    if (val < min) return;
	    max = val;
	}

	public void setMin(float val) {
	    if (val > max) return;
	    min = val;
	}

    @Override public int getParamCount() {
        return Settings.values().length; }

    @Override public String getName() {
        return LFO.class.getSimpleName(); }

    @Override
    public float get(int idx) {
        if (idx == Settings.Target.ordinal())
            return target.ordinal();
        if (idx == Settings.Min.ordinal())
            return getMin();
        if (idx == Settings.Max.ordinal())
            return getMax();
        if (idx == Settings.MSec.ordinal())
            return getFrequency();
        throw new InvalidParameterException();
    }

    @Override
    public void set(int idx, float value) {
        if (idx == Settings.Target.ordinal())
            target = Target.values()[(int)value];
        else if (idx == Settings.Min.ordinal())
            setMin(value);
        else if (idx == Settings.Max.ordinal())
            setMax(value);
        else if (idx == Settings.MSec.ordinal())
            setFrequency(value);
        else throw new InvalidParameterException();
    }

	/** called every MidiScheduler.LFO_PULSE jack frames in RT thread */
	public static void pulse() {
		for (LineIn ch : JudahZone.getChannels())
			if (ch.getLfo().isActive())
				lfoPulse.add(ch);
		for (Sample s : JudahZone.getLooper())
			if (s.getLfo().isActive())
				lfoPulse.add(s);
		if (JudahZone.getMasterTrack().getLfo().isActive())
		    lfoPulse.add(JudahZone.getMasterTrack());
		if (lfoPulse.isEmpty()) return;
		new Thread() { @Override public void run() {
			for (Channel ch : new ArrayList<>(lfoPulse))
				if (ch != null) ch.getLfo().pulse(ch);
			lfoPulse.clear();
		}}.start();
	}

	/** query the oscillator at a specific time, System.currentTimeMillis(), for example. */
	public double query(long millis) {
		// divide current millis + offset by frequency and apply sin wave, multiply by amplitude
		long time = millis + shift;
		double phase = (time % frequency) / frequency;
		double wave = 1 + Math.sin(Math.toRadians(phase * 360));
		return (wave * ( (max - min) / 2)) + min;
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
			case Pan: ch.setPan(val / 100f);
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

		long sleep = 80;
		final long spells = 50;
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
			System.out.println("done. min: " + min + " max: " + max + " frequency: " + lfo.getFrequency());
		}
		public static void main(String[] args) {
			new LFOTest(1200).start(); // 3 second LFO
		}
	}

}
