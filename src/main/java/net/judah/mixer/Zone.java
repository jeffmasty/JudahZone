package net.judah.mixer;

import static net.judah.JudahZone.*;
import static net.judah.gui.MainFrame.setFocus;

import java.util.ArrayList;
import java.util.Vector;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.looper.Looper;

/**Used for initialization/customization */
@RequiredArgsConstructor
public class Zone extends Vector<LineIn> {

	@Getter private final ArrayList<Instrument> instruments = new ArrayList<>();

	public Zone(LineIn... instruments) {
		for (LineIn input : instruments)
			add(input);
		preamps();
        mutes();
        prepare();
	}

	void prepare() {
		for (int i = 0; i < size(); i++) {
			if (get(i) instanceof Instrument processMe)
				instruments.add(processMe);
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
        getTaco().setMuteRecord(false);
        getDrumMachine().setMuteRecord(false); // individual kits can capture
	}

	void preamps() {
		getMains().getGain().setPreamp(Mains.PREAMP);
		getSampler().getGain().setPreamp(2.5f);
		getMic().getGain().setGain(0.25f); // trim studio noise
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
