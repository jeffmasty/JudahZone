package net.judah.mixer;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;

@RequiredArgsConstructor
public class Zone extends ArrayList<LineIn> {

	private ArrayList<Instrument> instruments;
	
	public LineIn getSource(String name) {
		for (LineIn ch : this)
			if (name.equals(ch.getName()))
				return ch;
		return null;
	}

	public ArrayList<Instrument> getInstruments() {
		if (instruments == null) {
			instruments = new ArrayList<>();
			for (Channel ch : this) 
				if (ch instanceof Instrument)
					instruments.add((Instrument)ch);
		}
		return instruments;
	}

	public Channel byName(String search) {
		for (LineIn in : this)
			if (in.getName().equals(search))
				return in;
		return null;
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
		JudahZone.getCrave().getGain().setGain(0.5f);
		JudahZone.getSynth1().getGain().setGain(0.5f);
		JudahZone.getSynth2().getGain().setGain(0.5f);
	}
	
}
