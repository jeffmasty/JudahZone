package net.judah.mixer;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.song.Cmdr;
import net.judah.song.Param;

@RequiredArgsConstructor
public class Zone extends ArrayList<LineIn> implements Cmdr {

	@Getter private final ArrayList<Instrument> instruments = new ArrayList<>();
	@Getter private String[] keys;
	
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
	
	public void init() {
		initVolume();
        initMutes();
        prepare();
	}
	
	/** By default, don't record drum track, microphone, sequencer */
    public void initMutes() {
        JudahZone.getMic().setMuteRecord(true);
        JudahZone.getDrumMachine().setMuteRecord(true);
        JudahZone.getSynth2().setMuteRecord(true);
	}
	public void initVolume() {
		JudahZone.getMic().getGain().setGain(0.3f);
		JudahZone.getFluid().getGain().setGain(0.5f);
		JudahZone.getGuitar().getGain().setGain(0.5f);
		JudahZone.getCrave().getGain().setGain(0.66f);
		JudahZone.getSynth1().getGain().setGain(0.5f);
		JudahZone.getSynth2().getGain().setGain(0.5f);
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
				ch.getLatchEfx().latch();
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
