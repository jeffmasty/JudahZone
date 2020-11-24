package net.judah.looper;

import lombok.Data;
import net.judah.jack.ProcessAudio;
import net.judah.jack.ProcessAudio.Type;

@Data
public class LoopSettings {

	private final ProcessAudio.Type type = Type.CONTROLLED;
	private final float scalingFactor = 1f;
	private final Recorder source;
	
}
