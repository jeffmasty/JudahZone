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
		guitar = new Channel("guitar", "system:capture_1", "guitar");
		guitar.setGainFactor(2.5f);
		guitar.setVolume(60);
		guitar.setDefaultCC(MPK.KNOBS.get(2)); // CC16
		
		mic = new Channel("mic", "system:capture_2", "mic");
		mic.setGainFactor(3.5f);
		mic.setVolume(0);
		mic.setDefaultCC(MPK.KNOBS.get(3)); // CC17

		drums = new Channel("drums", "system:capture_4", "drums");
		drums.getCompression().setActive(true);
		drums.setGainFactor(5.5f);
		drums.setVolume(55);
		// drums.setDefaultCC( MPK.knob.4 ) // see StageCommands

		synth = new Channel("synth",
				new String[] {FluidSynth.LEFT_PORT, FluidSynth.RIGHT_PORT}, 
				new String[] {"synthL", "synthR"});
		synth.setGainFactor(0.75f);
		synth.setVolume(40);
		synth.setDefaultCC(MPK.KNOBS.get(7));
		
		aux1 = new Channel("aux1", "system:capture_3", "aux1"); // boss dr5 drum machine
		aux1.getCompression().setActive(true);
		aux1.setGainFactor(2f);
		aux1.setVolume(35);
		aux1.setDefaultCC(MPK.KNOBS.get(4)); 
		
		aux2 = new Channel("aux2", new String[]
				{null, null}, // {"Calf Fluidsynth:Out L", "Calf Fluidsynth:Out R"} not started up yet here 
				new String[] {"aux2", "aux3"});
		// aux2.setMuteRecord(true);
		aux2.setGainFactor(3f);
		aux2.setVolume(88); // for octaver plugin
		aux2.setDefaultCC(MPK.KNOBS.get(5));

		addAll(Arrays.asList(new Channel[] { guitar, mic, drums, synth, aux1, aux2 }));
		
	}

	public Channel byName(String name) {
		for (Channel c : this)
			if (c.getName().equals(name)) 
				return c;
		return null;
	}

}
