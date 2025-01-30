package net.judah.mixer;

import static net.judah.JudahZone.*;
import static net.judah.gui.MainFrame.setFocus;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Vector;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.looper.Looper;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.Param;

/**Used for initialization/customization */
@RequiredArgsConstructor
public class Zone extends Vector<LineIn> implements Cmdr {

	@Getter private final ArrayList<Instrument> instruments = new ArrayList<>();
	@Getter private String[] keys;

	public Zone(LineIn... instruments) {
		for (LineIn input : instruments)
			add(input);
		preamps();
        mutes();
        prepare();
	}

	void prepare() {
		keys = new String[size()];
		for (int i = 0; i < keys.length; i++) {
			Channel ch = get(i);
			keys[i] = ch.getName();
			if (ch instanceof Instrument)
				instruments.add((Instrument)ch);
		}
	}

	public LineIn getSource(String name) {
		for (LineIn ch : this)
			if (name.equals(ch.getName()))
				return ch;
		return null;
	}

	public LineIn byName(String search) {
		for (LineIn in : this)
			if (in.getName().equals(search))
				return in;
		return null;
	}

	/** By default, don't record drum track, microphone, sequencer */
    public void mutes() {
        getGuitar().setMuteRecord(false);
        getTacos().taco.setMuteRecord(false);
        getDrumMachine().setMuteRecord(false); // individual kits can capture
	}

	void preamps() {
		getMains().getGain().setPreamp(Mains.PREAMP);
		getMic().getGain().setGain(0.25f); // trim studio noise
	}

	@Override
	public void execute(Param p) {
		LineIn ch = resolve(p.val);
		switch (p.cmd) {
			case OffTape:
				ch.setMuteRecord(true);
				break;
			case OnTape:
				ch.setMuteRecord(false);
				break;
			case Latch:
				JudahZone.getLooper().syncFx(ch);
				break;
			case SoloCh:
				JudahZone.getLooper().getSoloTrack().setSoloTrack(resolve(p.val));
				break;
			default: throw new InvalidParameterException("" + p);
		}
	}

	@Override
	public LineIn resolve(String key) {
		for (LineIn line : this)
			if (line.getName().equals(key))
				return line;
		return null;
	}

    public boolean nextChannel(boolean toRight) {
    	Looper looper = getLooper();
        Channel bus = getFxRack().getChannel();
        if (bus instanceof Instrument) {
            int i = indexOf(bus);
            if (toRight) {
                if (i == size() -1) {
                    setFocus(looper.get(0));
                    return true;
                }
                setFocus(get(i + 1));
                return true;
            } // else toLeft
            if (i == 0) {
                setFocus(looper.get(looper.size()-1));
                return true;
            }
            setFocus(get(i - 1));
            return true;
        }
        // else instanceof Sample
        int i = looper.indexOf(bus);
        if (toRight) {
            if (i == looper.size() - 1) {
                setFocus(get(0));
                return true;
            }
            setFocus(looper.get(i + 1));
            return true;
        } // else toLeft
        if (i == 0) {
            setFocus(get(size() - 1));
            return true;
        }
        setFocus(looper.get(i - 1));
        return true;
    }
}
