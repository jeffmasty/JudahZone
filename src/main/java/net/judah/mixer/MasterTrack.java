package net.judah.mixer;

import static net.judah.util.AudioTools.*;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.effects.api.Reverb;
import net.judah.util.AudioTools;

/**The unified effects/volume track just before hitting the speakers/external effects.
 * A master track initializes in a muted state.*/
public class MasterTrack extends Channel {

	final JackPort speakersLeft, speakersRight, effectsL, effectsR;
	FloatBuffer left, right;

	public MasterTrack(JackPort left, JackPort right,
	        JackPort effectsL, JackPort effectsR, Reverb reverb) {
		super("MAIN");
		this.speakersLeft = left;
		this.speakersRight = right;
		this.effectsL = effectsL;
		this.effectsR = effectsR;
		setReverb(reverb);
		setOnMute(true);
	}

	public void process() {
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
            processAdd(left, getGainL(), effectsL.getFloatBuffer());
            processAdd(right, getGainR(), effectsR.getFloatBuffer());
            processSilence(left);
            processSilence(right);
		}
		else {
		    AudioTools.processGain(speakersLeft.getFloatBuffer(), getGainL());
		    AudioTools.processGain(speakersRight.getFloatBuffer(), getGainR());
		}

	}

	public float getGainL() {
	    return (volume / 50f) * (1 - pan);
	}

	public float getGainR() {
	    return (volume / 50f) * pan;
	}
}
