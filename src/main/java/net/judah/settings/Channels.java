package net.judah.settings;

import java.util.ArrayList;
import java.util.Arrays;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.mixer.LineIn;
import net.judah.settings.MidiSetup.IN;
import net.judah.util.Icons;

@Getter
public class Channels extends ArrayList<LineIn> {
	public static final String GUITAR = "GUITAR";
	public static final String MIC = "MIC";
	public static final String DRUMS = "DRUMS";
	public static final String SYNTH = "SYNTH";
	public static final String AUX = "AUX";
	public static final String CRAVE = "CRAVE";
	
	private final LineIn guitar, mic, synth, crave /*, aux2 */;
	private final LineIn drums;

	public Channels() {

		guitar = new LineIn(GUITAR, "system:capture_1", "guitar");
		guitar.setIcon(Icons.load("Guitar.png"));

		mic = new LineIn(MIC, "system:capture_2", "mic");
		mic.setIcon(Icons.load("Microphone.png"));

		drums = new LineIn(DRUMS, "system:capture_4", "drums");
		// drums.getCompression().setActive(true);
		drums.setIcon(Icons.load("Drums.png"));

		synth = new LineIn(SYNTH,
				new String[] {FluidSynth.LEFT_PORT, FluidSynth.RIGHT_PORT},
				new String[] {"synthL", "synthR"});
		synth.setIcon(Icons.load("Synth.png"));

		crave = new LineIn(CRAVE, "system:capture_3", IN.CRAVE_IN.port); 
		crave.setIcon(Icons.load("Crave.png"));
		//aux1.setIcon(Icons.load("LoopA.png"));
		
		// aux1.getCompression().setActive(true);

//		aux2 = new LineIn(AUX, new String[]
//				{null, null}, // {"Calf Fluidsynth:Out L", "Calf Fluidsynth:Out R"} not started up yet here
//				new String[] {"aux2", "aux3"});
		//aux2.setIcon(Icons.load("LoopB.png"));
		// aux2.setMuteRecord(true);

		addAll(Arrays.asList(new LineIn[] { guitar, mic, synth, drums, crave, /* aux2 */ }));

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
		drums.getGain().setVol(50);
		synth.getGain().setVol(50);
		crave.getGain().setVol(33);
		//aux2.getGain().setVol(50);
		// drums.getCompression().setActive(true);
	}

    public static String volumeTarget(JackPort midiOut) {
        JudahMidi midi = JudahMidi.getInstance();
        Channels channels = JudahZone.getChannels();
        if (midiOut == midi.getCraveOut())
            return channels.getCrave().getName();
        else if (midiOut == midi.getCalfOut())
            return channels.getDrums().getName();
        else if (midiOut == midi.getDrumsOut())
            return channels.getDrums().getName();
        else if (midiOut == midi.getSynthOut())
            return channels.getSynth().getName();
        return "?";
    }

    public static void setVolume(int vol, JackPort midiOut) {
        JudahMidi midi = JudahMidi.getInstance();
        Channels channels = JudahZone.getChannels();
        if (midiOut == midi.getAuxOut1())
            channels.getCrave().getGain().setVol(vol);
        else if (midiOut == midi.getCalfOut())
            channels.getDrums().getGain().setVol(vol);
        else if (midiOut == midi.getDrumsOut())
            channels.getDrums().getGain().setVol(vol);
        else if (midiOut == midi.getSynthOut())
            channels.getSynth().getGain().setVol(vol);
    }

    public static int getVolume(JackPort midiOut) {
        JudahMidi midi = JudahMidi.getInstance();
        Channels channels = JudahZone.getChannels();
        if (midiOut == midi.getAuxOut1())
            return channels.getCrave().getVolume();
        else if (midiOut == midi.getCalfOut())
            return channels.getDrums().getVolume();
        else if (midiOut == midi.getDrumsOut())
            return channels.getDrums().getVolume();
        else if (midiOut == midi.getSynthOut())
            return channels.getSynth().getVolume();
        return 0;
    }



}
