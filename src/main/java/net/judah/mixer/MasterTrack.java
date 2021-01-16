package net.judah.mixer;

import static net.judah.util.AudioTools.*;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.JudahZone;
import net.judah.mixer.bus.Reverb;
import net.judah.util.AudioTools;

/**The unified effects/volume track just before hitting the speakers/external effects.
 * A master track initializes in a muted state.*/
public class MasterTrack extends Channel {

	final JackPort speakersLeft, speakersRight, effectsL, effectsR;
	FloatBuffer left, right;

	public MasterTrack(JackPort left, JackPort right,
	        JackPort effectsL, JackPort effectsR, Reverb reverb) {
		super(JudahZone.JUDAHZONE);
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

        if (delay.isActive()) {
            delay.processAdd(left, left, true);
            delay.processAdd(right, right, false);
        }

		if (reverb.isActive()) {
            processAdd(left, volume / 50f, effectsL.getFloatBuffer());
            processAdd(right, volume / 50f, effectsR.getFloatBuffer());
            processSilence(left);
            processSilence(right);
		}
		else if (volume != 50) {
			AudioTools.processGain(speakersRight.getFloatBuffer(), volume / 50f);
			AudioTools.processGain(speakersLeft.getFloatBuffer(), volume / 50f);
		}

	}

}
