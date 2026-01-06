package net.judah.mixer;

import static net.judah.gui.MainFrame.setFocus;

import java.util.ArrayList;
import java.util.Vector;

import judahzone.util.WavConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.channel.Instrument;
import net.judah.channel.LineIn;
import net.judah.looper.Looper;

/**Used for initialization/customization */
@RequiredArgsConstructor
public class Zone extends Vector<LineIn> {

	@Getter private final ArrayList<Instrument> instruments = new ArrayList<>();

	public Zone(JudahZone zone, LineIn... instruments) {
		for (LineIn input : instruments)
			add(input);
		preamps(zone);
        mutes(zone);
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
    public void mutes(JudahZone zone) {
        zone.getGuitar().setMuteRecord(false);
        zone.getTaco().setMuteRecord(false);
        zone.getDrumMachine().setMuteRecord(false); // individual kits can capture
	}

	void preamps(JudahZone zone) {
		zone.getMains().getGain().setPreamp(WavConstants.TO_LINE);
		zone.getDrumMachine().getGain().setPreamp((WavConstants.RUN_LEVEL + WavConstants.FILE_LEVEL) / 2);
		zone.getSampler().getGain().setPreamp(WavConstants.FILE_LEVEL);
		// zone.getBass().getGain().setPreamp(WavConstants.RUN_LEVEL);
		// TacoTruck gain.setPreamp(3f);
		// BasicPlayer.env 0.5f (unity?)
 	}

    public boolean nextChannel(boolean toRight) {
    	Looper looper = JudahZone.getInstance().getLooper();
        Channel bus = JudahZone.getInstance().getFxRack().getChannel();
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
