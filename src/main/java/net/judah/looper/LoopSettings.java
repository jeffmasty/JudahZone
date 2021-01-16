package net.judah.looper;

import lombok.Data;
import net.judah.api.ProcessAudio;
import net.judah.api.ProcessAudio.Type;

@Data
public class LoopSettings {

	private final ProcessAudio.Type type = Type.CONTROLLED;
	private final float scalingFactor = 1f;
	private final Recorder source;
	
}
