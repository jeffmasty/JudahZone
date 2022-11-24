package net.judah.effects;

import java.security.InvalidParameterException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.MainFrame;
import net.judah.effects.api.Effect;
import net.judah.effects.api.Reverb;
import net.judah.mixer.Channel;
import net.judah.util.RTLogger;

/** A calculated sin wave LFO.  Default amplitude returns queries between 0 and 85 */
@RequiredArgsConstructor @Getter @Setter
public class LFO implements Effect {

	public static final int LFO_MIN = 90;
    public static final int LFO_MAX = 1990;

	
    public enum Settings {
        Target, Min, Max, MSec
    }

	public static enum Target {
		OFF, CutEQ, Gain, Reverb, Delay, Pan
	};

	private final Channel ch;
	private boolean active;
	private Target target = Target.OFF;
	private int recover = -1;
	private boolean wasActive;
	
	
	/** in kilohertz (msec per cycle). default: oscillates over a 1.2 seconds. */
	@Getter private double frequency = 1200;
	/** align the wave on the jack frame buffer by millisecond (not implemented)*/
	private long shift;
	/** set maximum output level of queries. default: 90. */
	private int max = 90;
	/** set minimum level of queries. default 10. */
	private int min = 10;

    public void setTarget(Target target) {
    	if (this.target == target)
    		return;

		if (recover >= 0)
			switch (this.target) {
			case CutEQ:
				ch.getCutFilter().setFrequency(CutFilter.knobToFrequency(recover));
				ch.getCutFilter().setActive(wasActive);
				break;
			case Delay:
				ch.getDelay().set(Delay.Settings.DelayTime.ordinal(), recover);
				ch.getDelay().setActive(wasActive);
				break;
			case Reverb:
				ch.getReverb().set(Reverb.Settings.Wet.ordinal(), recover);
				ch.getReverb().setActive(wasActive);
				break;
			case Gain:
				ch.getGain().setVol(recover);
				break;
			case Pan:
				ch.getGain().setPan(recover);
				break;
			case OFF:
				break;
			}

		this.target = target;
		this.active = target != Target.OFF;

		if (active) {
			switch (this.target) {
			case CutEQ:
				recover = CutFilter.frequencyToKnob(ch.getCutFilter().getFrequency());
				wasActive = ch.getCutFilter().isActive();
				ch.getCutFilter().setActive(true);
				break;
			case Delay:
				recover = ch.getDelay().get(Delay.Settings.DelayTime.ordinal());
				wasActive = ch.getDelay().isActive();
				ch.getDelay().setActive(true);
				break;
			case Reverb:
				recover = ch.getReverb().get(Reverb.Settings.Wet.ordinal());
				wasActive = ch.getReverb().isActive();
				ch.getReverb().setActive(true);
				break;
			case Gain:
				recover = ch.getGain().getVol();
				break;
			case Pan:
				recover = ch.getGain().getPan();
				break;
			case OFF:
				break;
			}
		}		
		RTLogger.log(this, target.name());
		
	}

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
        	int result = ((int)( (getFrequency() - 90) / 19f));
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
	public void pulse() {
		if (!active) return;
		int val = (int)query();
		switch(target) {
			case Gain: ch.getGain().setVol(val); break;
			case CutEQ: ch.getCutFilter().setFrequency(
					CutFilter.knobToFrequency((int)ch.getLfo().query())); break;
			case Reverb: ch.getReverb().set(Reverb.Settings.Wet.ordinal(), val); break;
			case Delay: ch.getDelay().setFeedback(val * 0.01f); break;
			case Pan: ch.getGain().setPan(val); break;
			case OFF: return;
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
		final LFO lfo = new LFO(new Channel("test", false));

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
		
		public static void main2(String[] args) {
			new LFOTest(1200).start(); // 3 second LFO
		}
	}

	
	
}
