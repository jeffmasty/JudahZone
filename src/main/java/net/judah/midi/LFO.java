package net.judah.midi;

import java.security.InvalidParameterException;

import judahzone.api.FX;
import judahzone.api.TimeFX;
import judahzone.fx.Chorus;
import judahzone.fx.Delay;
import judahzone.fx.Filter;
import judahzone.fx.Gain;
import judahzone.fx.Reverb;
import judahzone.util.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.channel.Channel;
import net.judah.mixer.DJFilter;

/** A calculated sin wave LFO.  Default amplitude returns queries between 0 and 85 */
@RequiredArgsConstructor
public class LFO implements TimeFX {

    public enum Settings { Target, Min, Max, MSec, Type, Sync }

	public static enum Target {
		Pan, pArTy, Gain, Echo, Room, Delay, Chorus, Rate, Depth, Phase, Filtr }

	public static final int LFO_MIN = 50;
    public static final int LFO_MAX = 4500;

    private static int guiThrottle = 4;
	public static void setGuiThrottle(int count) { guiThrottle = count; }
	public static int getGuiThrottle() { return guiThrottle; }
    private long throttle; // count-up before presenting delta back to gui

	private final Channel ch;
	@Getter private final String name;
	@Setter @Getter String type = TYPE[0];
	@Setter @Getter boolean sync;
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

    public void setTarget(Target target) {
    	if (this.target == target)
    		return;
    	boolean active = ch.isActive(this);
    	if (active)
    		reset();
		this.target = target;
		if (active)
			activate();
	}
    @Override
	public void reset() {
    	if (recover < 0)
    		return;
    	switch (target) {
			case pArTy:
				ch.getDjFilter().joystick(50);
				break;
			case Delay:
				ch.getDelay().set(Delay.Settings.DelayTime.ordinal(), recover);
				ch.setActive(ch.getDelay(), wasActive);
				break;
			case Echo:
				ch.getReverb().set(Reverb.Settings.Wet.ordinal(), recover);
				ch.setActive(ch.getReverb(), wasActive);
				break;
			case Room:
				ch.getReverb().set(Reverb.Settings.Room.ordinal(), recover);
				ch.setActive(ch.getReverb(), wasActive);
				break;
			case Gain:
				ch.getGain().set(Gain.VOLUME, recover);
				break;
			case Pan:
				ch.getGain().set(Gain.PAN, recover);
				break;
			case Chorus:
				ch.getChorus().set(Chorus.Settings.Feedback.ordinal(), recover);
				ch.setActive(ch.getChorus(), wasActive);
				break;
			case Depth:
				ch.getChorus().set(Chorus.Settings.Depth.ordinal(), recover);
				ch.setActive(ch.getChorus(), wasActive);
				break;
			case Rate:
				ch.getChorus().set(Chorus.Settings.Rate.ordinal(), recover);
				ch.setActive(ch.getChorus(), wasActive);
				break;
			case Phase:
				ch.getChorus().set(Chorus.Settings.Phase.ordinal(), recover);
				ch.setActive(ch.getChorus(), wasActive);
				break;
			case Filtr:
				ch.getHiCut().set(Filter.Settings.Hz.ordinal(), recover);
				ch.setActive(ch.getHiCut(), wasActive);
				break;
    	}
    }

    @Override
	public void activate() {
    	throttle = 0;
    	switch (target) {
		case pArTy:
			recover = 50;
			wasActive = ch.isActive(ch.getHiCut());
			ch.setActive(ch.getHiCut(), true);
			break;
		case Delay:
			recover = ch.getDelay().get(Delay.Settings.DelayTime.ordinal());
			wasActive = ch.isActive(ch.getDelay());

			ch.setActive(ch.getDelay(), true);
			break;
		case Echo:
			recover = ch.getReverb().get(Reverb.Settings.Wet.ordinal());
			wasActive = ch.isActive(ch.getReverb());
			ch.setActive(ch.getReverb(), true);
			break;
		case Room:
			recover = ch.getReverb().get(Reverb.Settings.Room.ordinal());
			wasActive = ch.isActive(ch.getReverb());
			ch.setActive(ch.getReverb(), true);
			break;
		case Gain:
			recover = ch.getGain().get(Gain.VOLUME);
			break;
		case Pan:
			recover = ch.getGain().get(Gain.PAN);
			break;
    	case Chorus:
    		recover = ch.getChorus().get(Chorus.Settings.Feedback.ordinal());
			wasActive = ch.isActive(ch.getChorus());
			ch.setActive(ch.getChorus(), true);
			break;
    	case Depth:
    		recover = ch.getChorus().get(Chorus.Settings.Depth.ordinal());
			wasActive = ch.isActive(ch.getChorus());
			ch.setActive(ch.getChorus(), true);
			break;
    	case Rate:
    		recover = ch.getChorus().get(Chorus.Settings.Rate.ordinal());
			wasActive = ch.isActive(ch.getChorus());
			ch.setActive(ch.getChorus(), true);
			break;
    	case Phase:
    		recover = ch.getChorus().get(Chorus.Settings.Phase.ordinal());
    		wasActive = ch.isActive(ch.getChorus());
    		ch.setActive(ch.getChorus(), true);
    		break;
    	case Filtr:
    		recover = ch.getHiCut().get(Filter.Settings.Hz.ordinal());
    		wasActive = ch.isActive(ch.getHiCut());
    		ch.setActive(ch.getHiCut(), true);
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
        }
        if (idx == Settings.Type.ordinal())
        	return TimeFX.indexOf(type);
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
        else if (idx == Settings.Type.ordinal() && value < TimeFX.TYPE.length)
        	type = TimeFX.TYPE[value];
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
	public FX pulse() {
		if (!ch.isActive(this))
			return null;
		int val = (int)query();

		FX fx = switch (target) {
	    case Gain -> {
	        Gain g = ch.getGain();
	        g.set(Gain.VOLUME, val);
	        yield g;
	    }
	    case pArTy -> {
	        DJFilter f = ch.getDjFilter();
	        f.joystick(val);
	        yield null;
	    }
	    case Echo -> {
	        Reverb r = ch.getReverb();
	        r.set(Reverb.Settings.Wet.ordinal(), val);
	        yield r;
	    }
	    case Room -> {
	        Reverb r = ch.getReverb();
	        r.set(Reverb.Settings.Room.ordinal(), val);
	        yield r;
	    }
	    case Delay -> {
	        Delay d = ch.getDelay();
	        d.setFeedback(val * 0.01f);
	        yield d;
	    }
	    case Pan -> {
	        Gain g = ch.getGain();
	        g.set(Gain.PAN, val);
	        yield g;
	    }
	    case Chorus -> {
	        Chorus c = ch.getChorus();
	        c.set(Chorus.Settings.Feedback.ordinal(), val);
	        yield c;
	    }
	    case Depth -> {
	        Chorus c = ch.getChorus();
	        c.set(Chorus.Settings.Depth.ordinal(), val);
	        yield c;
	    }
	    case Rate -> {
	        Chorus c = ch.getChorus();
	        c.set(Chorus.Settings.Rate.ordinal(), val);
	        yield c;
	    }
	    case Phase -> {
	        Chorus c = ch.getChorus();
	        c.set(Chorus.Settings.Phase.ordinal(), val);
	        yield c;
	    }
	    case Filtr -> {
	        Filter h = ch.getHiCut();
	        h.set(Filter.Settings.Hz.ordinal(), val);
	        yield h;
	    }
	    default -> null;
	    };

		if (++throttle > guiThrottle) {
			throttle = 0;
			return fx;
		}
	    return null;
	}

	@Override public void sync(float unit) {
		frequency = 2 * (unit + unit * TimeFX.indexOf(type));
	}

	/** no-op, handled through MidiScheduler */
	@Override public void process(float[] left, float[] right) { }

    public void tremelo(int data2) {
		int center = ch.getVolume();
		int ratio = (int) (data2 * 0.01f * 50); // if center is zero, max tremelo is 0 to 100
		setMin(center - ratio);
		setMax(center + ratio);
		ch.setActive(this, data2 > 0);
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
		ch.setActive(this, data2 > 0);
	}

}


// saw wave
//private final static float TWOPI = (float) (2 * Math.PI);
//public double querySaw() {
//	long time = System.currentTimeMillis() + shift;
//	double phase = (time % frequency) / frequency;
//    double t = phase / TWOPI;
//    return (2.0f * t) - 1.0f;
