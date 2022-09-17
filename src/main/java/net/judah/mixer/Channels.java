package net.judah.mixer;

import java.util.ArrayList;
import java.util.Arrays;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.util.Constants;
import net.judah.util.Icons;

@Getter
public class Channels extends ArrayList<LineIn> {
	public static final String GUITAR = "GTR"; 
	public static final String MIC = "MIC";
	
	public static final String CALF= "DRUM";
	public static final String SYNTH = "KEYS";
	public static final String AUX = "AUX";
	public static final String CRAVE = "BASS";
	
	private Instrument guitar, mic, fluid;
	private Instrument crave, calf; 
	
	private void miniSetup() {
		guitar = new Instrument(GUITAR, "system:capture_1", "guitar");
		guitar.setIcon(Icons.load("Guitar.png"));
		mic = new Instrument(MIC, "system:capture_2", "mic");
		mic.setIcon(Icons.load("Microphone.png"));

		fluid = new Instrument(SYNTH,
				new String[] {FluidSynth.LEFT_PORT, FluidSynth.RIGHT_PORT},
				new String[] {"fluidL", "fluidR"});
		fluid.setIcon(Icons.load("Synth.png"));

		calf = new Instrument(CALF, new String[] {null, null}, new String[] {"calfL", "calfR"});
		calf.setIcon(Icons.load("Drums.png"));

		addAll(Arrays.asList(new Instrument[] { guitar, mic, fluid, calf, crave}));
	}
	
	public Channels() {
		if (Constants.getDi().contains("Komplete")) {
			miniSetup(); 
			return;
		}
		
		guitar = new Instrument(GUITAR, "system:capture_1", "guitar");
		guitar.setIcon(Icons.load("Guitar.png"));

		mic = new Instrument(MIC, "system:capture_4", "mic");
		mic.setIcon(Icons.load("Microphone.png"));

		fluid = new Instrument(SYNTH,
				new String[] {FluidSynth.LEFT_PORT, FluidSynth.RIGHT_PORT},
				new String[] {"fluidL", "fluidR"});

		calf = new Instrument(CALF, new String[] {null, null}, new String[] {"calfL", "calfR"});
		crave = new Instrument(CRAVE, "system:capture_3", "crave_in");
		
		addAll(Arrays.asList(new Instrument[] { guitar, mic, fluid, calf, crave}));
	}

	public Channel byName(String name) {
		for (Channel c : this)
			if (c.getName().equals(name))
				return c;
		return null;
	}

	public void initVolume() {
		if (mic != null) mic.getGain().setVol(30);
		fluid.getGain().setVol(30);
		calf.getGain().setVol(40);
		crave.getGain().setVol(40);
		//uno.getGain().setVol(40);
		guitar.getGain().setVol(60);
		
	}

	/** By default, don't record drum track, microphone, sequencer */
    public void initMutes() {
	    getCalf().setMuteRecord(true);
        if (mic != null) getMic().setMuteRecord(true);
        // if (circuit != null) getCircuit().setMuteRecord(true);
	}

    public ArrayList<Instrument> getInstruments() {
    	ArrayList<Instrument> result = new ArrayList<>();
    	for (LineIn ch : this)
    		if (ch instanceof Instrument)
    			result.add((Instrument)ch);
    	return result;
    }

    
    // deprecated
    public static String volumeTarget(JackPort midiOut) {
        JudahMidi midi = JudahMidi.getInstance();
        Channels channels = JudahZone.getChannels();
        if (midiOut == midi.getCraveOut())
            return channels.getCrave().getName();
        if (midiOut == midi.getCalfOut())
            return channels.getCalf().getName();
        else if (midiOut == midi.getFluidOut())
            return channels.getFluid().getName();
        return "?";
    }
    public static void setVolume(int vol, JackPort midiOut) {
        JudahMidi midi = JudahMidi.getInstance();
        Channels channels = JudahZone.getChannels();
        if (midiOut == midi.getCalfOut())
            channels.getCalf().getGain().setVol(vol);
        else if (midiOut == midi.getFluidOut())
            channels.getFluid().getGain().setVol(vol);
    }
    public static int getVolume(JackPort midiOut) {
        JudahMidi midi = JudahMidi.getInstance();
        Channels channels = JudahZone.getChannels();
        if (midiOut == midi.getCalfOut())
            return channels.getCalf().getVolume();
        else if (midiOut == midi.getFluidOut())
            return channels.getFluid().getVolume();
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
