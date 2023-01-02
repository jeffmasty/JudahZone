package net.judah.mixer;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.gui.Icons;
import net.judah.util.AudioTools;

/**The unified effects/volume track just before hitting the speakers/external effects.
 * A master track initializes in a muted state.*/
public class Mains extends Channel {

	final JackPort speakersLeft, speakersRight, effectsL, effectsR;

	public Mains(JackPort left, JackPort right) {
		this(left, right, null, null);
	}
	
	public Mains(JackPort left, JackPort right,
	        JackPort effectsL, JackPort effectsR) {
		super("MAIN", true);
		setIcon(Icons.get("Speakers.png"));
		this.speakersLeft = left;
		this.speakersRight = right;
		this.effectsL = effectsL;
		this.effectsR = effectsR;
		setOnMute(true);
	}

	public void process() {
		FloatBuffer left, right;
	    left = speakersLeft.getFloatBuffer();
	    right = speakersRight.getFloatBuffer();

        hiCut.process(left, right);
        cutFilter.process(left, right);
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
            delay.processAdd(left, left, true);
            delay.processAdd(right, right, false);
        }
		if (reverb.isActive()) {
			reverb.process(left, right);
		}
	    AudioTools.processGain(left, getGainL());
	    AudioTools.processGain(right, getGainR());
	}

	public float getGainL() {
	    return gain.getVol() * 0.01f * 0.5f * (1 - getPan());
	}

	public float getGainR() {
	    return gain.getVol() * 0.01f * 0.5f * getPan();
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
