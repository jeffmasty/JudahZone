package net.judah.mixer;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.looper.ToDisk;
import net.judah.omni.AudioTools;
import net.judah.omni.Icons;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

/**The unified effects/volume track just before hitting the speakers/external effects.
 * A master track initializes in a muted state.*/
public class Mains extends Channel {

	public static final float PREAMP = 13f;

	@Getter private ToDisk tape;
	@Getter private boolean hotMic;
	@Setter private boolean copy;

	public Mains() {
		super(Mains.class.getSimpleName(), true);
		icon = Icons.get("Speakers.png");
	}


	@Override
	public void process() {
		// no-op
	}

	public void process(FloatBuffer left, FloatBuffer right) {

		gain.process(left, right);
	    stream().filter(fx->fx.isActive()).forEach(fx->fx.process(left, right));

		if (tape != null)
			tape.offer(left, right);
		if (copy) { // put out a read buffer like other channels offer (RMS meters)
			copy = false;
			AudioTools.copy(left, getLeft().array());
			AudioTools.copy(right, getRight().array());
		}
	}

	public boolean isRecording() { return tape != null; }

	public void tape(boolean openFileDialog) {
		if (tape == null && openFileDialog) {
			File f = Folders.choose(new File(System.getProperty("user.home")));
			if (f == null) return;
			try {
				tape = new ToDisk(f);
			} catch (IOException e) { RTLogger.warn(this, e); }
		}
		else
			tape();
	}

	public void tape() {
		try {
			if (tape == null)
				tape = new ToDisk();
			else {
				ToDisk old = tape;
				tape = null;
				old.close();
			}
		} catch (IOException e) { RTLogger.warn(this, e); }
	}

	public void hotMic() {
		hotMic = !hotMic;
		MainFrame.update(this);
	}

}


//	if (reverb.isInternal())
//		((Freeverb)reverb).process(left, right);
//	else { // external reverb
//        mix(left, getGainL(), effectsL.getFloatBuffer());
//        mix(right, getGainR(), effectsR.getFloatBuffer());
//        silence(left);silence(right);
