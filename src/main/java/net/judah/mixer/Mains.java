package net.judah.mixer;

import static net.judah.util.AudioTools.*;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.effects.Freeverb;
import net.judah.effects.api.Reverb;
import net.judah.util.AudioTools;
import net.judah.util.Icons;

/**The unified effects/volume track just before hitting the speakers/external effects.
 * A master track initializes in a muted state.*/
public class Mains extends Channel {

	final JackPort speakersLeft, speakersRight, effectsL, effectsR;

	public Mains(JackPort left, JackPort right) {
		this(left, right, null, null, null);
	}
	
	public Mains(JackPort left, JackPort right,
	        JackPort effectsL, JackPort effectsR, Reverb reverb) {
		super("MAIN", true);
		setIcon(Icons.load("Speakers.png"));
		this.speakersLeft = left;
		this.speakersRight = right;
		this.effectsL = effectsL;
		this.effectsR = effectsR;
		if (reverb != null) setReverb(reverb);
		setOnMute(true);
	}

	public void process() {
		FloatBuffer left, right;
	    left = speakersLeft.getFloatBuffer();
	    right = speakersRight.getFloatBuffer();

        if (eq.isActive()) {
            eq.process(left, true);
            eq.process(right, false);
        }

        if (cutFilter.isActive()) {
            cutFilter.process(left, right, 1);
        }

        if (chorus.isActive())
            chorus.processStereo(left, right);

        if (overdrive.isActive()) {
            overdrive.processAdd(left);
            overdrive.processAdd(right);
        }

        if (delay.isActive()) {
            delay.processAdd(left, left, true);
            delay.processAdd(right, right, false);
        }

		if (reverb.isActive()) {
			if (reverb.isInternal()) 
				((Freeverb)reverb).process(left, right);
			else { // external reverb
	            mix(left, getGainL(), effectsL.getFloatBuffer());
	            mix(right, getGainR(), effectsR.getFloatBuffer());
	            silence(left);
	            silence(right);
	            return;
			}
		}
	    AudioTools.processGain(speakersLeft.getFloatBuffer(), getGainL());
	    AudioTools.processGain(speakersRight.getFloatBuffer(), getGainR());
	}

	public float getGainL() {
	    return gain.getVol() * 0.01f * 0.5f * (1 - getPan());
	}

	public float getGainR() {
	    return gain.getVol() * 0.01f * 0.5f * getPan();
	}
}
