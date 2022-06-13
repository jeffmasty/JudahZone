package net.judah.settings;

import java.util.ArrayList;
import java.util.Arrays;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.mixer.LineIn;
import net.judah.util.Icons;

@Getter
public class Channels extends ArrayList<LineIn> {
	public static final String GUITAR = "GUITAR"; 
	public static final String MIC = "MIC";
	public static final String CIRCUIT = "TRAX";
	public static final String UNO = "UNO";
	
	public static final String CALF= "CALF";
	public static final String SYNTH = "SYNTH";
	public static final String AUX = "AUX";
	public static final String CRAVE = "CRAVE";
	
	private final LineIn guitar, mic, fluid;
	private final LineIn uno, circuit, calf; 
	private final LineIn crave; //aux1,2

	public Channels() {

		guitar = new LineIn(GUITAR, "system:capture_1", "guitar");
		guitar.setIcon(Icons.load("Guitar.png"));

		mic = new LineIn(MIC, "system:capture_4", "mic");
		mic.setIcon(Icons.load("Microphone.png"));

		fluid = new LineIn(SYNTH,
				new String[] {FluidSynth.LEFT_PORT, FluidSynth.RIGHT_PORT},
				new String[] {"fluidL", "fluidR"});
		fluid.setIcon(Icons.load("Synth.png"));

		calf = new LineIn(CALF, new String[] {null, null}, new String[] {"calfL", "calfR"});
		calf.setIcon(Icons.load("Drums.png"));

		uno = new LineIn(UNO, 
				new String[] {"system:capture_7", "system:capture_8"},
				new String[] {"UnoL", "UnoR"});

		crave = new LineIn(CRAVE, "system:capture_3", "crave_in");
		
		circuit = new LineIn("TRAX", 
				new String[] {"system:capture_5", "system:capture_6"},
				new String[] {"CircuitL", "CircuitR"});
		
		addAll(Arrays.asList(new LineIn[] { guitar, mic, fluid, calf, uno, crave, circuit}));

	}

	public LineIn byName(String name) {
		for (LineIn c : this)
			if (c.getName().equals(name))
				return c;
		return null;
	}

	public void initVolume() {
		mic.getGain().setVol(10);
		guitar.getGain().setVol(50);
		fluid.getGain().setVol(33);
		calf.getGain().setVol(40);
		uno.getGain().setVol(40);
	}

	/** By default, don't record drum track, microphone, sequencer */
    public void initMutes() {
	    getCalf().setMuteRecord(true);
        getMic().setMuteRecord(true);
        getCircuit().setMuteRecord(true);
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

    
}
