package net.judah.mixer;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.FileChooser;
import net.judah.looper.ToDisk;
import net.judah.util.AudioTools;
import net.judah.util.RTLogger;

/**The unified effects/volume track just before hitting the speakers/external effects.
 * A master track initializes in a muted state.*/
public class Mains extends Channel {
	private final float BOOST = 12f;
	
	private final JackPort speakersLeft, speakersRight;
	@Getter private ToDisk tape;
	@Getter private boolean hotMic;
	@Setter private boolean copy;
	
	public Mains(JackPort left, JackPort right) {
		super("MAIN", true);
		icon = Icons.get("Speakers.png");
		this.speakersLeft = left;
		this.speakersRight = right;
	}

	public Mains(JackPort left, JackPort right, String preset) {
		this(left, right);
		setPreset(preset);
	}
	
	public void process() {
		FloatBuffer left = speakersLeft.getFloatBuffer();
	    FloatBuffer right = speakersRight.getFloatBuffer();

	    AudioTools.gain(left, (1 - gain.getStereo()) * (gain.getGain() * BOOST));
	    AudioTools.gain(right, gain.getStereo() * (gain.getGain() * BOOST));

        filter2.process(left, right);
        filter1.process(left, right);
        if (chorus.isActive())
            chorus.processStereo(left, right);

        if (compression.isActive()) {
        	compression.process(left);
        	compression.process(right);
        }
        
        if (overdrive.isActive()) {
            overdrive.processAdd(left);
            overdrive.processAdd(right);
        }
	    if (eq.isActive()) {
            eq.process(left, true);
            eq.process(right, false);
        }

        if (delay.isActive()) {
        	delay.process(left, right);
        }
		if (reverb.isActive()) {
			reverb.process(left, right);
		}
		if (tape != null)
			tape.offer(left, right);
		if (copy) {
			copy = false;
			AudioTools.copy(left, getLeft().array());
			AudioTools.copy(right, getRight().array());
		}
	}

	public boolean isRecording() { return tape != null; }
	
	public void tape(boolean openFileDialog) {
		if (tape == null && openFileDialog) {
			File f = FileChooser.choose(new File(System.getProperty("user.home")));
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
//        silence(left);
//        silence(right);
//        return;
//	}
