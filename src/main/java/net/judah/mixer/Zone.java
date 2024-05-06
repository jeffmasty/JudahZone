package net.judah.mixer;

import static net.judah.JudahZone.*;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Vector;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.midi.fluid.FluidSynth.Tracks;
import net.judah.seq.track.MidiTrack;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.Param;
import net.judah.util.Constants;

@RequiredArgsConstructor
public class Zone extends Vector<LineIn> implements Cmdr {

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
        getMic().setMuteRecord(true);
        getDrumMachine().setMuteRecord(true);
        getSynth2().setMuteRecord(true);
	}
	public void initVolume() {
		getMic().getGain().setGain(0.3f);
		getFluid().getGain().setGain(0.5f);
		getGuitar().getGain().setGain(0.5f);
		getBass().getGain().setGain(0.5f);
		getSynth1().getGain().setGain(0.5f);
		getSynth2().getGain().setGain(0.5f);
	}
	
	public void initSynths() {
		getSynth1().progChange("FeelGood");
		getSynth2().progChange("Drops1");
		getSynth1().getTracks().get(0).load("0s");
		getSynth2().getTracks().get(0).load("16ths");
		getBass().getTracks().get(0).load("Bass2");
		
		while (getFluid().getChannels().isEmpty())
			Constants.sleep(100); // allow time for FluidSynth to sync TODO timeout
		Vector<MidiTrack> tracks = getFluid().getTracks();
		MidiTrack fluid1 = tracks.get(Tracks.Fluid1.ordinal());
		fluid1.load("8ths");
		fluid1.getMidiOut().progChange("Strings", fluid1.getCh());
		MidiTrack fluid2 = tracks.get(Tracks.Fluid2.ordinal());
		fluid2.load("CRDSKNK");
		fluid2.getMidiOut().progChange("Palm Muted Guitar", fluid2.getCh());
		MidiTrack fluid3 = tracks.get(Tracks.Fluid3.ordinal());
		fluid3.load("Synco");
		fluid3.getMidiOut().progChange("Harp", fluid3.getCh());

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
