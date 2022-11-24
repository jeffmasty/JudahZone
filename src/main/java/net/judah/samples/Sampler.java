package net.judah.samples;

import java.util.ArrayList;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.api.ProcessAudio.Type;
import net.judah.drumz.StepSample;
import net.judah.effects.Fader;
import net.judah.util.RTLogger;

@Getter
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

	private final ArrayList<StepSample> stepSamples = new ArrayList<>();
	private StepSample stepSample;
	
	public Sampler(JackPort left, JackPort right) {
		for (int i = 0; i < NAMES.length; i++) {
			try {
				add(new Sample(left, right, NAMES[i], i < 4 ? Type.FREE : Type.ONE_SHOT));
			} catch (Exception e) {
				RTLogger.warn(this, e);
			}
		}
		try {
			stepSamples.add(new StepSample("Crickets", 4, 12));
			stepSamples.add(new StepSample("Shaker", 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15));
			stepSamples.add(new StepSample("Claves", 4, 10, 14));
			stepSamples.add(new StepSample("Snares", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
			stepSample = stepSamples.get(0);
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
	}
	
	
    /** play and/or record loops and samples in Real-Time thread */
	public void process() {
    	for (Sample sample : this)
    		sample.process();
    	for (StepSample s : stepSamples)
    		if (s.isOn())
    			s.process();
    }

	public void play(Sample s, boolean on) {
		if (on) {
			if (s.getType() == Type.ONE_SHOT) 
				s.setTapeCounter(0);
			else 
				Fader.execute(Fader.fadeIn(s));
			s.setActive(true);
		}
		else {
			if (s.getType() == Type.ONE_SHOT) 
				s.setActive(false);
			else 
				Fader.execute(Fader.fadeOut(s));
		}
		MainFrame.update(s);
	}

	public boolean isStepping() {
		for (StepSample s : stepSamples)
			if (s.isOn())
				return true;
		return false;
	}
	
	public void stepperOff() {
		for (StepSample s : stepSamples) 
			s.setOn(false);
	}
	
	public void stepSample(int idx) {
		if (idx < 0 || idx >= stepSamples.size())
			return;
		
		boolean wasOn = false;
		if (stepSample != null) {
			wasOn = stepSample.isOn();
			stepSample.setOn(false);
		}
		stepSample = stepSamples.get(idx);
		stepSample.setOn(wasOn);
		RTLogger.log(this, stepSample.getName());
	}
	
	public void nextStepSample() {
		if (stepSample == null)
			stepSample = stepSamples.get(stepSamples.size() -1);
		int current = stepSamples.indexOf(stepSample) + 1;
		if (current >= stepSamples.size())
			current = 0;
		stepSample(current);
	}
}

	
//	private JComboBox<String> stepper;
//	public JComboBox<String> getStepper() {
//		if (stepper == null) {
//			stepper = new CenteredCombo<>();
//			for (StepSample s : stepSamples)
//				stepper.addItem(s.getName());
//			stepper.setSelectedIndex(0);
//			stepper.addActionListener(e-> {
//				if (isStepping() == false) return;
//				stepSample(stepper.getSelectedIndex());});
//		}
//		return stepper;
//	}
