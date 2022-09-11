package net.judah.looper;

import java.io.File;

import lombok.Getter;

/** currently, plays ((crickets)) on 2 and 4 */
public class Sample extends Loop {

	@Getter final protected File file;
	
	public Sample(String name, File f, Looper looper) throws Exception {
		super(name, looper);
		this.file = f;
		setRecording(WavFile.load(file));
		dirty = false;
		type = Type.ONE_SHOT;
	}

	public void step(int step) {
		if (step == 4 || step == 12) {
			setTapeCounter(0);
			dirty = true;
		}
	}
	
}
