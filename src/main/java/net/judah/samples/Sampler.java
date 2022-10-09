package net.judah.samples;

import java.util.ArrayList;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.MainFrame;
import net.judah.api.ProcessAudio.Type;
import net.judah.effects.Fader;
import net.judah.util.RTLogger;

public class Sampler extends ArrayList<Sample> {

	public static final String[] NAMES = new String[] {
			"Creek", "Rain", "Birds", "Bicycle", // loops
			"FeelGoodInc", "Prrrrrr", "DropDaBass", "DJOutlaw"}; // one-shots
	// Fountain Creek: Fields Park, Manitou Springs, CO
	// Rain in Cuba: https://freesound.org/people/kyles/sounds/362077/
	// Birds: https://freesound.org/people/hargissssound/sounds/345851/
	// Bicycle https://freesound.org/people/bojan_t95/sounds/507013/
	// Prrrrrr: https://freesound.org/people/Duisterwho/sounds/644104/
	// DropDaBass: https://freesound.org/people/qubodup/sounds/218891/
	// DJOutlaw: https://freesound.org/people/tim.kahn/sounds/94748/

	public Sampler(JackPort left, JackPort right) {
		for (int i = 0; i < NAMES.length; i++) {
			try {
				add(new Sample(left, right, NAMES[i], i < 4 ? Type.FREE : Type.ONE_SHOT));
			} catch (Exception e) {
				RTLogger.warn(this, e);
			}
		}
	}
	
    /** play and/or record loops and samples in Real-Time thread */
	public void process() {
    	for (int i = 0; i < size(); i++)
    		get(i).process();
    }

	public void play(Sample s, boolean on) {
		if (on) {
			if (s.getType() == Type.ONE_SHOT) 
				s.setTapeCounter(0);
			else {
				Fader.execute(Fader.fadeIn(s));
			}
			s.setActive(true);
		}
		else {
			if (s.getType() == Type.ONE_SHOT) {
				s.setActive(false);
			}
			else {
				Fader.execute(Fader.fadeOut(s));
			}
		}
		MainFrame.update(s);
	}

}
