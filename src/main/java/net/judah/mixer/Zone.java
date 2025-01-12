package net.judah.mixer;

import static net.judah.JudahZone.*;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Vector;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
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
		initVolume();
        initMutes();
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
    public void initMutes() {
        getGuitar().setMuteRecord(false);
        getTacos().taco.setMuteRecord(false);
	}

	public void initVolume() {
		getMic().getGain().setGain(0.3f);
		getFluid().getGain().setGain(0.5f);
		getGuitar().getGain().setGain(0.5f);
		getBass().getGain().setGain(0.5f);
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
}
