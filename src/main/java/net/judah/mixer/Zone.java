package net.judah.mixer;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.api.Engine;
import net.judah.drumkit.DrumKit;
import net.judah.midi.MidiInstrument;
import net.judah.synth.JudahSynth;

@RequiredArgsConstructor
public class Zone extends ArrayList<LineIn> {

	public LineIn getSource(String name) {
		for (LineIn ch : this)
			if (name.equals(ch.getName()))
				return ch;
		return null;
	}

	public ArrayList<Instrument> getInstruments() {
		ArrayList<Instrument> result = new ArrayList<>();
		for (Channel ch : this) {
			if (ch instanceof Instrument)
				result.add((Instrument)ch);
		}
		return result;
	}

	public ArrayList<Engine> getInternal() {
		ArrayList<Engine> result = new ArrayList<>();
		result.addAll(getSynths());
		result.addAll(getDrums());
		return result;
	}
	
	public ArrayList<JudahSynth> getSynths() {
		ArrayList<JudahSynth> result = new ArrayList<>();
		for (Channel ch : this) 
			if (ch instanceof JudahSynth)
				result.add((JudahSynth)ch);
		return result;
	}

	public ArrayList<DrumKit> getDrums() {
		ArrayList<DrumKit> result = new ArrayList<>();
		for (Channel ch : this) 
			if (ch instanceof DrumKit)
				result.add((DrumKit)ch);
		return result;
	}
	
	public ArrayList<MidiInstrument> getGMs() {
		ArrayList<MidiInstrument> result = new ArrayList<>();
		for (Channel ch : this) 
			if (ch instanceof MidiInstrument)
				result.add((MidiInstrument)ch);
		return result;
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
		JudahZone.getMic().getGain().setVol(30);
		JudahZone.getFluid().getGain().setVol(50);
		JudahZone.getGuitar().getGain().setVol(50);
		JudahZone.getCrave().getGain().setVol(50);
		JudahZone.getSynth1().getGain().setVol(50);
		JudahZone.getSynth2().getGain().setVol(50);
	}
	
}
