package net.judah.fx;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;
import net.judah.util.Constants;

/** A calculated sin wave LFO.  Default amplitude returns queries between 0 and 85 */
@RequiredArgsConstructor
public class LFO implements TimeEffect {

	public static final int LFO_MIN = 50;
    public static final int LFO_MAX = 4500;

    long throttle;
	@Setter @Getter String type = TYPE[0];
	@Setter @Getter boolean sync;

    public enum Settings { Target, Min, Max, MSec, Type, Sync }

	public static enum Target {
		Pan, pArTy, Gain, Echo, Room, Delay, Chorus, Rate, Depth, Phase, Filtr }

	private final Channel ch;
	@Getter private boolean active;
	@Getter private Target target = Target.Pan;
	/** in kiloHertz (msec per cycle). default: oscillates over a 1.2 seconds. */
	@Getter private double frequency = 1200;
	/** set maximum output level of queries. default: 90. */
	private int max = 90;
	/** set minimum level of queries. default 10. */
	private int min = 10;
	private int recover = -1;
	private boolean wasActive;

    @Override public int getParamCount() {
        return Settings.values().length; }

    @Override public String getName() {
        return LFO.class.getSimpleName(); }

	public void setMax(int val) {
		if (val > 100) val = 100;
	    if (val < min) return;
	    max = val;
	}

	public void setMin(int val) {
		if (val < 0) val = 0;
	    if (val > max) return;
	    min = val;
	}

	@Override public void setActive(boolean active) {
		if (this.active == active)
			return;
		if (active)
			setup();
		else
			recover();
		this.active = active;
		MainFrame.update(ch);
	}

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
			case pArTy:
				ch.getDjFilter().joystick(50);
				break;
			case Delay:
				ch.getDelay().set(Delay.Settings.DelayTime.ordinal(), recover);
				ch.getDelay().setActive(wasActive);
				break;
			case Echo:
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
			case Phase:
				ch.getChorus().set(Chorus.Settings.Phase.ordinal(), recover);
				ch.getChorus().setActive(wasActive);
				break;
			case Filtr:
				ch.getHiCut().set(Filter.Settings.Hz.ordinal(), recover);
				ch.getHiCut().setActive(wasActive);
				break;

    	}
    }

    private void setup() {
		throttle = 0;
    	switch (target) {
		case pArTy:
			recover = 50;
			wasActive = ch.getHiCut().isActive();
			ch.getHiCut().setActive(true);
			break;
		case Delay:
			recover = ch.getDelay().get(Delay.Settings.DelayTime.ordinal());
			wasActive = ch.getDelay().isActive();
			ch.getDelay().setActive(true);
			break;
		case Echo:
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
    	case Phase:
    		recover = ch.getChorus().get(Chorus.Settings.Phase.ordinal());
    		wasActive = ch.getChorus().isActive();
    		ch.getChorus().setActive(true);
    		break;
    	case Filtr:
    		recover = ch.getHiCut().get(Filter.Settings.Hz.ordinal());
    		wasActive = ch.getHiCut().isActive();
    		ch.getHiCut().setActive(true);
    	}
    }
    @Override public int get(int idx) {
        if (idx == Settings.Target.ordinal())
            return target.ordinal();
        if (idx == Settings.Min.ordinal())
            return min;
        if (idx == Settings.Max.ordinal())
            return max;
        if (idx == Settings.MSec.ordinal()) {
        	return Constants.reverseLog((float)frequency, LFO_MIN, LFO_MAX);
			//return ((int)( (frequency - MAX_FACTOR) / MAX_FACTOR)); // linear map 39 to 3990msec to 0 to 100
        }
        if (idx == Settings.Type.ordinal())
        	return TimeEffect.indexOf(type);
        if (idx == Settings.Sync.ordinal())
        	return sync ? 1 : 0;
        // TODO Shape <| |> ~~
        throw new InvalidParameterException();
    }

    @Override public void set(int idx, int value) {
        if (idx == Settings.Target.ordinal())
        	setTarget(Target.values()[value]);
        else if (idx == Settings.Min.ordinal())
            setMin(value);
        else if (idx == Settings.Max.ordinal())
            setMax(value);
        else if (idx == Settings.MSec.ordinal())
        	frequency = Constants.logarithmic(value, LFO_MIN, LFO_MAX);
        	// frequency = value * MAX_FACTOR + MAX_FACTOR;   // linear 39msec to 3990msec
        else if (idx == Settings.Type.ordinal() && value < TimeEffect.TYPE.length)
        	type = TimeEffect.TYPE[value];
        else if (idx == Settings.Sync.ordinal())
        	sync = value > 0;
        else throw new InvalidParameterException();
    }

	/** query the oscillator at a specific time, System.currentTimeMillis(), for example. */
	public double query(long millis) {
		// divide current millis (+ offset?) by frequency and apply sin wave, multiply by amplitude
		double phase = (millis % frequency) / frequency;
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
			case pArTy: ch.getDjFilter().joystick(val); break;
			case Echo: ch.getReverb().set(Reverb.Settings.Wet.ordinal(), val); break;
			case Room: ch.getReverb().set(Reverb.Settings.Room.ordinal(), val); break;
			case Delay: ch.getDelay().setFeedback(val * 0.01f); break;
			case Pan: ch.getGain().set(Gain.PAN, val); break;
			case Chorus: ch.getChorus().set(Chorus.Settings.Feedback.ordinal(), val); break;
			case Depth: ch.getChorus().set(Chorus.Settings.Depth.ordinal(), val); break;
			case Rate: ch.getChorus().set(Chorus.Settings.Rate.ordinal(), val); break;
			case Phase: ch.getChorus().set(Chorus.Settings.Phase.ordinal(), val); break;
			case Filtr: ch.getHiCut().set(Filter.Settings.Hz.ordinal(), val); break;

		}
		if (++throttle > 4) {// throttle gui updates
			throttle = 0;
			MainFrame.update(ch);
		}
	}

	@Override public void sync(float unit) {
		frequency = 2 * (unit + unit * TimeEffect.indexOf(type));
	}

	@Override public void sync() {
		sync(TimeEffect.unit());
	}

	/** no-op, handled through MidiScheduler */
	@Override public void process(FloatBuffer left, FloatBuffer right) { }

	public LFO(Channel channel, Target filter) {
		this(channel);
		setTarget(target);
	}

    public void tremelo(int data2) {
		int center = ch.getVolume();
		int ratio = (int) (data2 * 0.01f * 50); // if center is zero, max tremelo is 0 to 100
		setMin(center - ratio);
		setMax(center + ratio);
		setActive(data2 > 0);
    }

    /** Set an LFO preset on Chorus Phase, min/max based off val */
	public void phaser(int data2) {
		// Preset:	LFO(9/?Min/?Max/45/0/0)
		set(Settings.Target.ordinal(), Target.Phase.ordinal());
		set(Settings.MSec.ordinal(), 45);
		set(Settings.Type.ordinal(), 0);
		set(Settings.Sync.ordinal(), 0);

		int offset = (int) (data2 * 0.5f * Constants.TO_100);
		set(Settings.Min.ordinal(), 50 - offset);
		set(Settings.Max.ordinal(), 50 + offset);
		setActive(data2 > 0);
	}

}


// saw wave
//private final static float TWOPI = (float) (2 * Math.PI);
//public double querySaw() {
//	long time = System.currentTimeMillis() + shift;
//	double phase = (time % frequency) / frequency;
//    double t = phase / TWOPI;
//    return (2.0f * t) - 1.0f;
