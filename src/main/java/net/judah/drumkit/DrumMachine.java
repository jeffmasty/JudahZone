package net.judah.drumkit;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.Icons;
import net.judah.gui.knobs.KitKnobs;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;

@Getter 
public class DrumMachine extends LineIn {
	
	private final DrumKit drum1, drum2, hats, fills;
	@Getter private final DrumKit[] kits;
	@Getter private final KitKnobs knobs = new KitKnobs();
	/** current midi controller input and view */
	@Setter @Getter private int current;
	
	public DrumMachine(String name, JackPort outL, JackPort outR, String icon) {
		super(name, true);
		setIcon(Icons.get(icon));
		setLeftPort(outL);
		setRightPort(outR);
		
		drum1 = new DrumKit(KitMode.Drum1);
		drum2 = new DrumKit(KitMode.Drum2);
		hats = new DrumKit(KitMode.Hats);
		fills = new DrumKit(KitMode.Fills);
		kits = new DrumKit[] {drum1, drum2, hats, fills};
		knobs.setKit(drum1);
		setPreset("Drumz");
		setPresetActive(true);
	}

	// process+mix each drumkit, process this channel's fx, place on mains
	public void process() {
		AudioTools.silence(left);
		AudioTools.silence(right);
		for (DrumKit kit : kits) {
			kit.process();
			AudioTools.mix(kit.getLeft(), left);
			AudioTools.mix(kit.getRight(), right);
		}
		processStereoFx(gain.getGain());
		AudioTools.mix(left, leftPort.getFloatBuffer()); 
        AudioTools.mix(right, rightPort.getFloatBuffer());  
	}
	
	public DrumKit increment() {
		current++;
		if (current >= kits.length)
			current = 0;
		return kits[current];
	}
	
	public DrumKit getActive() {
		return kits[current];
	}

}
