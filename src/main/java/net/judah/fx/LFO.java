package net.judah.fx;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;

/** A calculated sin wave LFO.  Default amplitude returns queries between 0 and 85 */
@RequiredArgsConstructor
public class LFO implements TimeEffect {

	public static final int LFO_MIN = 90;
    public static final int LFO_MAX = 1990;
    static final float MAX_FACTOR = 39f;
	long throttle;
	@Setter @Getter String type = TYPE[0];
	@Setter @Getter boolean sync;

    public enum Settings {
        Target, Min, Max, MSec, Type, Sync
    }

	public static enum Target {
		Pan, Filter, Gain, Reverb, Room, Delay, Chorus, Rate, Depth
	};

	private final Channel ch;
	@Getter private boolean active;
	@Getter private Target target = Target.Pan;
	private int recover = -1;
	private boolean wasActive;

	@Override
	public void setActive(boolean active) {
		if (this.active == active)
			return;
		if (active)
			setup();
		else
			recover();
		this.active = active;
		MainFrame.update(ch);
	}
	/** in kilohertz (msec per cycle). default: oscillates over a 1.2 seconds. */
	@Setter @Getter private double frequency = 1200;
	/** align the wave on the jack frame buffer by millisecond (not implemented)*/
	private long shift;
	/** set maximum output level of queries. default: 90. */
	private int max = 90;
	/** set minimum level of queries. default 10. */
	private int min = 10;

    public void setTarget(Target target) {
    	if (this.target == target)
    		return;
    	if (active)
    		recover();
		this.target = target;
		if (active)
			setup();
	}

    private void recover() {
    	if (!active || recover < 0)
    		return;
    	switch (target) {
			case Filter:
				ch.getFilter1().setFrequency(Filter.knobToFrequency(recover));
				ch.getFilter1().setActive(wasActive);
				break;
			case Delay:
				ch.getDelay().set(Delay.Settings.DelayTime.ordinal(), recover);
				ch.getDelay().setActive(wasActive);
				break;
			case Reverb:
				ch.getReverb().set(Reverb.Settings.Wet.ordinal(), recover);
				ch.getReverb().setActive(wasActive);
				break;
			case Room:
				ch.getReverb().set(Reverb.Settings.Room.ordinal(), recover);
				ch.getReverb().setActive(wasActive);
				break;
			case Gain:
				ch.getGain().set(Gain.VOLUME, recover);
				break;
			case Pan:
				ch.getGain().set(Gain.PAN, recover);
				break;
			case Chorus:
				ch.getChorus().set(Chorus.Settings.Feedback.ordinal(), recover);
				ch.getChorus().setActive(wasActive);
				break;
			case Depth:
				ch.getChorus().set(Chorus.Settings.Depth.ordinal(), recover);
				ch.getChorus().setActive(wasActive);
				break;
			case Rate:
				ch.getChorus().set(Chorus.Settings.Rate.ordinal(), recover);
				ch.getChorus().setActive(wasActive);
				break;

    	}
    }

    private void setup() {
		throttle = 0;
    	switch (target) {
		case Filter:
			recover = Filter.frequencyToKnob(ch.getFilter1().getFrequency());
			wasActive = ch.getFilter1().isActive();
			ch.getFilter1().setActive(true);
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
		case Room:
			recover = ch.getReverb().get(Reverb.Settings.Room.ordinal());
			wasActive = ch.getReverb().isActive();
			ch.getReverb().setActive(true);
			break;
		case Gain:
			recover = ch.getGain().get(Gain.VOLUME);
			break;
		case Pan:
			recover = ch.getGain().get(Gain.PAN);
			break;
    	case Chorus:
    		recover = ch.getChorus().get(Chorus.Settings.Feedback.ordinal());
			wasActive = ch.getChorus().isActive();
    		ch.getChorus().setActive(true);
			break;
    	case Depth:
    		recover = ch.getChorus().get(Chorus.Settings.Depth.ordinal());
			wasActive = ch.getChorus().isActive();
    		ch.getChorus().setActive(true);
			break;
    	case Rate:
    		recover = ch.getChorus().get(Chorus.Settings.Rate.ordinal());
			wasActive = ch.getChorus().isActive();
    		ch.getChorus().setActive(true);
			break;
    	}
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
            return min;
        if (idx == Settings.Max.ordinal())
            return max;
        if (idx == Settings.MSec.ordinal()) {
        	// map 90 to 3990msec to 0 to 100
        	int result = ((int)( (getFrequency() - 90) / MAX_FACTOR));
        	if (result < 0) result = 0;
        	else if (result > 100) result = 100;
            return result;
        }
        if (idx == Settings.Type.ordinal())
        	return TimeEffect.indexOf(type);
        if (idx == Settings.Sync.ordinal())
        	return sync ? 1 : 0;
        // TODO Shape <| |> ~~
        throw new InvalidParameterException();
    }

    @Override
    public void set(int idx, int value) {
        if (idx == Settings.Target.ordinal())
        	setTarget(Target.values()[value]);
        else if (idx == Settings.Min.ordinal())
            setMin(value);
        else if (idx == Settings.Max.ordinal())
            setMax(value);
        else if (idx == Settings.MSec.ordinal())
        	frequency = value * MAX_FACTOR + 90;   // 90msec to 3990msec
        else if (idx == Settings.Type.ordinal() && value < TimeEffect.TYPE.length)
        	type = TimeEffect.TYPE[value];
        else if (idx == Settings.Sync.ordinal())
        	sync = value > 0;
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
			case Gain: ch.getGain().set(Gain.VOLUME, val); break;
			case Filter: ch.getFilter1().setFrequency(
					Filter.knobToFrequency((int)query())); break;
			case Reverb: ch.getReverb().set(Reverb.Settings.Wet.ordinal(), val); break;
			case Room: ch.getReverb().set(Reverb.Settings.Room.ordinal(), val); break;
			case Delay: ch.getDelay().setFeedback(val * 0.01f); break;
			case Pan: ch.getGain().set(Gain.PAN, val); break;
			case Chorus: ch.getChorus().set(Chorus.Settings.Feedback.ordinal(), val); break;
			case Depth: ch.getChorus().set(Chorus.Settings.Depth.ordinal(), val); break;
			case Rate: ch.getChorus().set(Chorus.Settings.Rate.ordinal(), val); break;

		}
		if (++throttle > 4) {// throttle gui updates
			throttle = 0;
			MainFrame.update(ch);
		}
	}

	@Override
	public void sync(float unit) {
		float msec = 2 * (unit + unit * TimeEffect.indexOf(type));
    	setFrequency(msec);
	}

	@Override
	public void sync() {
		sync(TimeEffect.unit());
	}

	/** no-op, handled through MidiScheduler */
	@Override
	public void process(FloatBuffer left, FloatBuffer right) { }

	public LFO(Channel channel, Target filter) {
		this(channel);
		setTarget(target);
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


//public static class LFOTest extends Thread {
//	final LFO lfo = new LFO(new Channel("test", false));
//
//	long sleep = 80;
//	final long spells = 50;
//	int count;
//
//	double min = Double.MAX_VALUE;
//	double max = Double.MIN_VALUE;
//
//	public LFOTest(int freq) {
//		lfo.frequency = freq;
//	}
//
//	@Override
//	public void run() {
//		while(count <= spells) {
//			double query = lfo.query();
//			if (query > max) max = query;
//			if (query < min) min = query;
//
//			long spaces = Math.round(query / 5);
//			String display = "";
//			for (int i = 0; i< spaces; i++)
//				display += " ";
//			display += "*";
//			System.out.println(display + "       " + query);
//
//			count++;
//			try { Thread.sleep(sleep);
//			} catch (Throwable t) { }
//		}
//		System.out.println("done. min: " + min + " max: " + max + " frequency: " + lfo.getFrequency());
//	}
//
//	public static void main2(String[] args) {
//		new LFOTest(1200).start(); // 3 second LFO
//	}
//}

