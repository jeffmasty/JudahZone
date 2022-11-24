package net.judah.drumz;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.controllers.KnobMode;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;
import net.judah.util.Icons;

@Getter 
public class DrumMachine extends LineIn {

	private final DrumKit drum1, drum2, hats, fills;
	private final DrumKit[] drumkits;
	@Setter private boolean armed; // TODO
	protected final FloatBuffer[] buffer = new FloatBuffer[] 
			{FloatBuffer.allocate(bufSize), FloatBuffer.allocate(bufSize)};
	
	public DrumMachine(String name, JackPort outL, JackPort outR, String icon) {
		super(name, true);
		setIcon(Icons.load(icon));
		setLeftPort(outL);
		setRightPort(outR);
		
		drum1 = new DrumKit(KnobMode.Drums1);
		drum2 = new DrumKit(KnobMode.Drums2);
		hats = new DrumKit(KnobMode.Hats);
		fills = new DrumKit(KnobMode.Fills);
		drumkits = new DrumKit[] {drum1, hats, drum2, fills};
		
	}

	public void process() {
		AudioTools.silence(buffer);
		for (DrumKit kit : drumkits) {
			kit.process(buffer);
			AudioTools.mix(kit.getBuffer()[0], buffer[0]);
			AudioTools.mix(kit.getBuffer()[1], buffer[1]);
		}

		processFx(buffer[0], buffer[1], 3 * gain.getGain());
	}
	
	
	

}
