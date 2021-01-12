package net.judah.mixer;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.jack.AudioTools;

public class MasterTrack extends Channel {

	final JackPort left, right;
	
	@Getter private int volume = 50;
	
	public MasterTrack(JackPort left, JackPort right) {
		super("ZONE");
		this.left = left;
		this.right = right;
		setOnMute(true);
	}

	FloatBuffer toJackLeft, toJackRight;
	public void process() {
		
		if (cutFilter.isActive()) {
			left.getFloatBuffer().rewind();
			right.getFloatBuffer().rewind();
			cutFilter.process(left.getFloatBuffer(), right.getFloatBuffer(), volume / 100f);
		}
		else if (volume != 50) {
			right.getFloatBuffer().rewind();
			AudioTools.processGain(right.getFloatBuffer(), volume / 50f);
			left.getFloatBuffer().rewind();
			AudioTools.processGain(left.getFloatBuffer(), volume / 50f);
		}
				
	}
	
}
