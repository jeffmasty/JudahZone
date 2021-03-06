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
	public static final String DRUMS = "DRUMS";
	public static final String SYNTH = "SYNTH";
	public static final String AUX1 = "AUX1";
	public static final String AUX2 = "AUX2";

	private final LineIn guitar, mic, synth, aux1, aux2;
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

		aux1 = new LineIn(AUX1, "system:capture_3", "aux1"); // boss dr5 drum machine
		// aux1.getCompression().setActive(true);

		aux2 = new LineIn(AUX2, new String[]
				{null, null}, // {"Calf Fluidsynth:Out L", "Calf Fluidsynth:Out R"} not started up yet here
				new String[] {"aux2", "aux3"});
		// aux2.setMuteRecord(true);

		addAll(Arrays.asList(new LineIn[] { guitar, mic, drums, synth, aux1, aux2 }));

	}

	public LineIn byName(String name) {
		for (LineIn c : this)
			if (c.getName().equals(name))
				return c;
		return null;
	}

	public void initVolume() {
		mic.setVolume(0);
		guitar.setVolume(60);
		drums.setVolume(55);
		synth.setVolume(40);
		aux1.setVolume(30);
		aux2.setVolume(65);
		drums.getCompression().setActive(true);
	}

    public static String volumeTarget(JackPort midiOut) {
        JudahMidi midi = JudahMidi.getInstance();
        Channels channels = JudahZone.getChannels();
        if (midiOut == midi.getAuxOut1())
            return channels.getAux1().getName();
        else if (midiOut == midi.getAuxOut2())
            return channels.getAux2().getName();
        else if (midiOut == midi.getAuxOut3())
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
            channels.getAux1().setVolume(vol);
        else if (midiOut == midi.getAuxOut2())
            channels.getAux2().setVolume(vol);
        else if (midiOut == midi.getAuxOut3())
            channels.getDrums().setVolume(vol);
        else if (midiOut == midi.getDrumsOut())
            channels.getDrums().setVolume(vol);
        else if (midiOut == midi.getSynthOut())
            channels.getSynth().setVolume(vol);
    }

    public static int getVolume(JackPort midiOut) {
        JudahMidi midi = JudahMidi.getInstance();
        Channels channels = JudahZone.getChannels();
        if (midiOut == midi.getAuxOut1())
            return channels.getAux1().getVolume();
        else if (midiOut == midi.getAuxOut2())
            return channels.getAux2().getVolume();
        else if (midiOut == midi.getAuxOut3())
            return channels.getDrums().getVolume();
        else if (midiOut == midi.getDrumsOut())
            return channels.getDrums().getVolume();
        else if (midiOut == midi.getSynthOut())
            return channels.getSynth().getVolume();
        return 0;
    }



}
