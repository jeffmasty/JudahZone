package net.judah;

import java.util.ArrayList;
import java.util.Arrays;

import lombok.Getter;
import net.judah.fluid.FluidSynth;
import net.judah.mixer.LineIn;
import net.judah.plugin.MPK;
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
		guitar.setGainFactor(2.5f);
		guitar.setDefaultCC(MPK.KNOBS.get(2)); // CC16
		
		mic = new LineIn(MIC, "system:capture_2", "mic");
		mic.setGainFactor(3.5f);
		mic.setIcon(Icons.load("Microphone.png"));
		mic.setDefaultCC(MPK.KNOBS.get(3)); // CC17

		drums = new LineIn(DRUMS, "system:capture_4", "drums");
		drums.getCompression().setActive(true);
		drums.setGainFactor(6f);
		drums.setIcon(Icons.load("Drums.png"));
		// drums.setDefaultCC( MPK.knob.4 ) // see StageCommands

		synth = new LineIn(SYNTH,
				new String[] {FluidSynth.LEFT_PORT, FluidSynth.RIGHT_PORT}, 
				new String[] {"synthL", "synthR"});
		synth.setGainFactor(0.75f);
		synth.setIcon(Icons.load("Synth.png"));
		synth.setDefaultCC(MPK.KNOBS.get(7));
		
		aux1 = new LineIn(AUX1, "system:capture_3", "aux1"); // boss dr5 drum machine
		aux1.getCompression().setActive(true);
		aux1.setGainFactor(3f);
		aux1.setDefaultCC(MPK.KNOBS.get(4)); 
		
		aux2 = new LineIn(AUX2, new String[]
				{null, null}, // {"Calf Fluidsynth:Out L", "Calf Fluidsynth:Out R"} not started up yet here 
				new String[] {"aux2", "aux3"});
		// aux2.setMuteRecord(true);
		aux2.setGainFactor(3f);
		aux2.setDefaultCC(MPK.KNOBS.get(5));

		addAll(Arrays.asList(new LineIn[] { guitar, mic, drums, synth, aux1, aux2 }));
		
	}

	public LineIn byName(String name) {
		for (LineIn c : this)
			if (c.getName().equals(name)) 
				return c;
		return null;
	}

	void initVolume() {
		mic.setVolume(0);
		guitar.setVolume(60);
		drums.setVolume(55);
		synth.setVolume(30);
		aux1.setVolume(30);
		aux2.setVolume(88); // for octaver plugin
	}

}
