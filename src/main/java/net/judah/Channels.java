package net.judah;

import java.util.ArrayList;
import java.util.Arrays;

import lombok.Getter;
import net.judah.fluid.FluidSynth;
import net.judah.mixer.Channel;
import net.judah.plugin.MPK;

@Getter 
public class Channels extends ArrayList<Channel> {
	
	private final Channel guitar, mic, synth, aux1, aux2;
	private final Channel drums;
	
	public Channels() {
		guitar = new Channel("GUITAR", "system:capture_1", "guitar");
		guitar.setGainFactor(2.5f);
		guitar.setDefaultCC(MPK.KNOBS.get(2)); // CC16
		
		mic = new Channel("MIC", "system:capture_2", "mic");
		mic.setGainFactor(3.5f);
		mic.setDefaultCC(MPK.KNOBS.get(3)); // CC17

		drums = new Channel("DRUMS", "system:capture_4", "drums");
		drums.getCompression().setActive(true);
		drums.setGainFactor(6f);
		// drums.setDefaultCC( MPK.knob.4 ) // see StageCommands

		synth = new Channel("SYNTH",
				new String[] {FluidSynth.LEFT_PORT, FluidSynth.RIGHT_PORT}, 
				new String[] {"synthL", "synthR"});
		synth.setGainFactor(0.75f);
		synth.setDefaultCC(MPK.KNOBS.get(7));
		
		aux1 = new Channel("AUX1", "system:capture_3", "aux1"); // boss dr5 drum machine
		aux1.getCompression().setActive(true);
		aux1.setGainFactor(3f);
		aux1.setDefaultCC(MPK.KNOBS.get(4)); 
		
		aux2 = new Channel("AUX2", new String[]
				{null, null}, // {"Calf Fluidsynth:Out L", "Calf Fluidsynth:Out R"} not started up yet here 
				new String[] {"aux2", "aux3"});
		// aux2.setMuteRecord(true);
		aux2.setGainFactor(3f);
		aux2.setDefaultCC(MPK.KNOBS.get(5));

		addAll(Arrays.asList(new Channel[] { guitar, mic, drums, synth, aux1, aux2 }));
		
	}

	public Channel byName(String name) {
		for (Channel c : this)
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
