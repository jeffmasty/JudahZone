package net.judah.mixer;

import java.util.ArrayList;
import java.util.Arrays;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.midi.JudahMidi;

@Getter @RequiredArgsConstructor
public class Channels extends ArrayList<Instrument> {
	public static final String GUITAR = "GTR"; 
	public static final String MIC = "MIC";
	public static final String CALF= "Calf";
	public static final String FLUID = "Fluid";
	public static final String CRAVE = "Crave";
	
	private final Instrument guitar, mic, crave;
	private final GMSynth fluid, calf;

	public static Channel byName(String name, ArrayList<Channel> channels) {
		for (Channel c : channels)
			if (c.getName().equals(name))
				return c;
		throw new NullPointerException(name + " " + Arrays.toString(channels.toArray()));
	}
	
	public Instrument byName(String name) {
		for (Instrument c : this)
			if (c.getName().equals(name))
				return c;
		return null;
	}

	public void initVolume() {
		if (mic != null) mic.getGain().setVol(30);
		fluid.getGain().setVol(30);
		calf.getGain().setVol(40);
		crave.getGain().setVol(40);
		guitar.getGain().setVol(60);
		JudahZone.getSynth().getGain().setVol(40);
		JudahZone.getSynth2().getGain().setVol(40);
		JudahZone.getBeats().getGain().setVol(50);

	}

	/** By default, don't record drum track, microphone, sequencer */
    public void initMutes() {
	    getCalf().setMuteRecord(true);
        if (mic != null) getMic().setMuteRecord(true);
        JudahZone.getBeats().setMuteRecord(true);
        JudahZone.getSynth2().setMuteRecord(true);
	}

    public ArrayList<Instrument> getInstruments() {
    	ArrayList<Instrument> result = new ArrayList<>();
    	for (LineIn ch : this)
    		if (ch instanceof Instrument)
    			result.add((Instrument)ch);
    	return result;
    }

    public void setVolume(int vol, JackPort midiOut) {
        JudahMidi midi = JudahZone.getMidi();
        if (midiOut == midi.getCalfOut())
            getCalf().getGain().setVol(vol);
        else if (midiOut == midi.getFluidOut())
            getFluid().getGain().setVol(vol);
    }
    public int getVolume(JackPort midiOut) {
        JudahMidi midi = JudahZone.getMidi();
        if (midiOut == midi.getCalfOut())
            return getCalf().getVolume();
        else if (midiOut == midi.getFluidOut())
            return getFluid().getVolume();
        return 0;
    }

    @Override
    public String toString() {
        String debug = "channels: ";
        for (Channel ch : this)
            debug += ch.getName() + " ";
        return debug;
    }
    
}
// public static final String CIRCUIT = "TRAX";
// public static final String UNO = "UNO";
// private LineIn circuit, uno, aux1;
// fluid.setIcon(Icons.load("Synth.png"));
// calf.setIcon(Icons.load("Drums.png"));
// uno = new LineIn(UNO, 
//		new String[] {"system:capture_7", "system:capture_8"},
//		new String[] {"UnoL", "UnoR"});
// circuit = new LineIn("TRAX", 
//		new String[] {"system:capture_5", "system:capture_6"},
//		new String[] {"CircuitL", "CircuitR"});
