package net.judah.drumkit;

import java.util.ArrayList;
import java.util.HashMap;

import javax.sound.midi.InvalidMidiDataException;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KitKnobs;
import net.judah.midi.JudahClock;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;

@Getter 
public class DrumMachine extends LineIn {

	private final DrumKit drum1, drum2, hats, fills;
	private final ArrayList<DrumKit> kits = new ArrayList<>();
	private final HashMap<DrumKit, KitKnobs> knobs = new HashMap<>();
	/** current midi controller input and view */
	@Setter private DrumKit current;

	public DrumMachine(JackPort outL, JackPort outR, JudahClock clock) throws InvalidMidiDataException {
		super("Drums", true);
		icon = Icons.get("DrumMachine.png");
		leftPort = outL;
		rightPort = outR;
		drum1 = new DrumKit(KitMode.Drum1, clock);
		drum2 = new DrumKit(KitMode.Drum2, clock);
		hats = new DrumKit(KitMode.Hats, clock);
		fills = new DrumKit(KitMode.Fills, clock);
		kits.add(drum1); kits.add(drum2); kits.add(hats); kits.add(fills);
		current = drum1;
		knobs.put(drum1, new KitKnobs(drum1));
		knobs.put(drum2, new KitKnobs(drum2));
		knobs.put(hats, new KitKnobs(hats));
		knobs.put(fills, new KitKnobs(fills));
		setPreset("Drumz");
	}

	public KitKnobs getKnobs() {
		return knobs.get(current);
	}

	public KitKnobs getKnobs(DrumKit kit) {
		return knobs.get(kit);
	}

	public Object getKnobs(KitMode mode) {
		for (KitKnobs k : knobs.values())
			if (k.getKit().getKitMode() == mode)
				return k;
		return null;
	}

	public void increment() {
		int idx = kits.indexOf(current) + 1;
		if (idx>= kits.size())
			idx = 0;
		current = kits.get(idx);
		MainFrame.setFocus(current);
	}

	// process + mix each drumkit, process this channel's fx, place on mains
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

}
