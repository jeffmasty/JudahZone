package net.judah.drumkit;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;
import net.judah.util.Icons;

@Getter 
public class DrumMachine extends LineIn {

	private final DrumKit drum1, drum2, hats, fills;
	@Getter private final DrumKit[] kits;
	/** current midi controller input and view */
	@Setter @Getter private int current;
	@Setter private boolean armed; // TODO
	protected final FloatBuffer[] buffer = new FloatBuffer[] 
			{FloatBuffer.allocate(bufSize), FloatBuffer.allocate(bufSize)};
	
	public DrumMachine(String name, JackPort outL, JackPort outR, String icon) {
		super(name, true);
		setIcon(Icons.load(icon));
		setLeftPort(outL);
		setRightPort(outR);
		
		drum1 = new DrumKit(KitMode.Drum1);
		drum2 = new DrumKit(KitMode.Drum2);
		hats = new DrumKit(KitMode.Hats);
		fills = new DrumKit(KitMode.Fills);
		kits = new DrumKit[] {drum1, drum2, hats, fills};
		
	}

	public void process() {
		AudioTools.silence(buffer);
		for (DrumKit kit : kits) {
			kit.process(buffer);
			AudioTools.mix(kit.getBuffer()[0], buffer[0]);
			AudioTools.mix(kit.getBuffer()[1], buffer[1]);
		}

		processFx(buffer[0], buffer[1], 3.5f * gain.getGain());
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
