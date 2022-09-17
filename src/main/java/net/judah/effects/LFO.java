package net.judah.effects;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.effects.api.Effect;
import net.judah.effects.api.Reverb;
import net.judah.looper.Looper;
import net.judah.mixer.Channel;
import net.judah.util.RTLogger;

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
	private Target target = Target.Pan;
	public void setTarget(Target target) {
		if (this.target != target) {
			this.target = target;
			RTLogger.log(this, target.name());
		}
	}

	/** in kilohertz (msec per cycle). default: oscillates over a 1.2 seconds. */
	@Getter private double frequency = 1200;

	/** align the wave on the jack frame buffer by millisecond (not implemented)*/
	private long shift;

	/** set maximum output level of queries. default: 90. */
	private int max = 90;
	/** set minimum level of queries. default 10. */
	private int min = 10;

    private static final ArrayList<Channel> lfoPulse = new ArrayList<>();

	public void setMax(int val) {
	    if (val < min) return;
	    max = val;
	}

	public void setMin(int val) {
	    if (val > max) return;
	    min = val;
	}

    @Override public int getParamCount() {
        return Settings.values().length; }

    @Override public String getName() {
        return LFO.class.getSimpleName(); }

    @Override
    public int get(int idx) {
        if (idx == Settings.Target.ordinal())
            return target.ordinal();
        if (idx == Settings.Min.ordinal())
            return getMin();
        if (idx == Settings.Max.ordinal())
            return getMax();
        if (idx == Settings.MSec.ordinal()) {
        	// map 90 to 1990 to 0 to 100
        	int result = ((int)(getFrequency() / 19f)) - 90;
        	if (result < 0) result = 0;
        	else if (result > 100) result = 100;
            return result;
        }
        throw new InvalidParameterException();
    }

    @Override
    public void set(int idx, int value) {
        if (idx == Settings.Target.ordinal()) {
        	Target old = target;
            target = Target.values()[value];
            if (target != old) 
            	RTLogger.log(this, target.name());}
        else if (idx == Settings.Min.ordinal())
            setMin(value);
        else if (idx == Settings.Max.ordinal())
            setMax(value);
        else if (idx == Settings.MSec.ordinal()) 
        	setFrequency(value * 19 + 90);   // 90msec to 1990msec
        else throw new InvalidParameterException();
    }

	/** called every MidiScheduler.LFO_PULSE jack frames in RT thread */
	public static void pulse() {
		synchronized (lfoPulse) {
			lfoPulse.clear();
			for (Channel ch : JudahZone.getChannels())
				if (ch.getLfo().isActive())
					lfoPulse.add(ch);
			if (JudahZone.getMains() == null)
				return;
			Looper looper = JudahZone.getLooper();
				for (int i = 0; i < looper.size(); i++)
					if (looper.get(i).getLfo().isActive())
						lfoPulse.add(looper.get(i));
			if (JudahZone.getMains().getLfo().isActive())
			    lfoPulse.add(JudahZone.getMains());
			if (lfoPulse.isEmpty()) return;
		}
		new Thread(() -> {
			for (Channel ch : new ArrayList<>(lfoPulse))
				if (ch != null) ch.getLfo().pulse(ch);
		}).start();
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
			case Gain: ch.getGain().setVol(val); break;
			case CutEQ: ch.getCutFilter().setFrequency(
					CutFilter.knobToFrequency((int)ch.getLfo().query())); break;
			case Reverb: ch.getReverb().set(Reverb.Settings.Wet.ordinal(), val); break;
			case Delay: ch.getDelay().setFeedback(val / 100f); break;
			case Pan: ch.getGain().setPan(val); break;
		}
		MainFrame.update(ch);
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
