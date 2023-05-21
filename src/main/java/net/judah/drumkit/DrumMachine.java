package net.judah.drumkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KitKnobs;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;

@Getter 
public class DrumMachine extends LineIn {
	
	private final DrumKit drum1, drum2, hats, fills;
	private final ArrayList<DrumKit> kits = new ArrayList<>();
	private final HashMap<DrumKit, KitKnobs> knobs = new HashMap<>();
	/** current midi controller input and view */
	@Setter private DrumKit current;
	
	public DrumMachine(String name, JackPort outL, JackPort outR, String icon) {
		super(name, true);
		setIcon(Icons.get(icon));
		setLeftPort(outL);
		setRightPort(outR);
		
		drum1 = new DrumKit(KitMode.Drum1);
		drum2 = new DrumKit(KitMode.Drum2);
		hats = new DrumKit(KitMode.Hats);
		fills = new DrumKit(KitMode.Fills);
		kits.add(drum1); kits.add(drum2); kits.add(hats); kits.add(fills);
		current = drum1;
		knobs.put(drum1, new KitKnobs(drum1));
		knobs.put(drum2, new KitKnobs(drum2));
		knobs.put(hats, new KitKnobs(hats));
		knobs.put(fills, new KitKnobs(fills));
		setPreset("Drumz");
		setPresetActive(true);
		
	}
	
	public KitKnobs getKnobs() {
		if (knobs.get(current) == null)
			throw new NullPointerException(current + " --- " + Arrays.toString(knobs.keySet().toArray()));
		return knobs.get(current);
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
	
	public void increment() {
		int idx = kits.indexOf(current) + 1;
		if (idx>= kits.size())
			idx = 0;
		current = kits.get(idx);
		MainFrame.setFocus(current);
	}
	
}
