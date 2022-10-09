package net.judah.drumz;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.Icons;

@Getter 
public class DrumMachine extends LineIn {

	private final DrumKit drum1, drum2, hats, fills;
	private final DrumKit[] channels;

	@Getter protected final FloatBuffer[] buffer = new FloatBuffer[] 
			{FloatBuffer.allocate(Constants.bufSize()), FloatBuffer.allocate(Constants.bufSize())};
	
	public DrumMachine(String name, JackPort outL, JackPort outR, String icon) {
		super(name, true);
		setIcon(Icons.load(icon));
		setLeftPort(outL);
		setRightPort(outR);
		
		drum1 = new DrumKit("Drum1");
		drum2 = new DrumKit("Drum2");
		hats = new DrumKit("Hats");
		fills = new DrumKit("Fills");
		channels = new DrumKit[] {drum1, drum2, hats, fills};
		
	}

	public void process() {
		AudioTools.silence(buffer);
		for (DrumKit kit : channels) {
			kit.process(buffer);
			AudioTools.mix(kit.getBuffer()[0], buffer[0]);
			AudioTools.mix(kit.getBuffer()[1], buffer[1]);
		}
		AudioTools.processGain(buffer[0], 2f); // boost
		AudioTools.processGain(buffer[1], 2f);

		processFx(buffer[0], buffer[1]);
	}
	
	
	

}
