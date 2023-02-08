package net.judah.drumkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.api.ProcessAudio.Type;
import net.judah.fx.Fader;
import net.judah.gui.MainFrame;
import net.judah.util.RTLogger;

@Getter
public class Sampler extends ArrayList<Sample> {
	
	// Fountain Creek: Fields Park, Manitou Springs, CO
	// Rain in Cuba: https://freesound.org/people/kyles/sounds/362077/
	// Birds: https://freesound.org/people/hargissssound/sounds/345851/
	// Bicycle https://freesound.org/people/bojan_t95/sounds/507013/
	public static final String[] LOOPS = {"Creek", "Rain", "Birds", "Bicycle"};
	
	// FeelGood: Gorilaz sample
	// Prrrrrr: https://freesound.org/people/Duisterwho/sounds/644104/
	// DropBass: https://freesound.org/people/qubodup/sounds/218891/
	// DJOutlaw: https://freesound.org/people/tim.kahn/sounds/94748/
	public static final String[] ONESHOTS = {"FeelGood", "Prrrrrr", "DropBass", "DJOutlaw"}; 
	
	public static final String[] NAMES = Stream.concat(
			Arrays.stream(LOOPS), Arrays.stream(ONESHOTS)).toArray(String[]::new);
	public static final int SIZE = NAMES.length;

	/** unified amplification for all samples */
	@Getter float mix = 0.6f;
	@Getter private int selected;
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
			stepSamples.add(new StepSample("Claves", 4, 10, 14));
			stepSamples.add(new StepSample("Ride", 0, 4, 8, 12));
			stepSamples.add(new StepSample("Shaker", 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15));
			stepSamples.add(new StepSample("Snares", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
			stepSamples.add(new StepSample("4x4", 0, 4, 8, 12));
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

	public void step(int step) {
		if (stepSample == null) return;
		stepSample.step(step);
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
	
	public void setSelected(int idx) {
		if (idx < 0 || idx >= stepSamples.size())
			return;
		
		boolean wasOn = false;
		if (stepSample != null) {
			wasOn = stepSample.isOn();
			stepSample.setOn(false);
		}
		stepSample = stepSamples.get(idx);
		stepSample.setOn(wasOn);
		selected = idx;
		MainFrame.update(this);
	}
	
	public void setStepping(boolean active) {
		if (active)
			stepSamples.get(selected).setOn(active);
		else
			stepperOff();
		MainFrame.update(this);
	}
	
	public void nextStepSample() {
		if (stepSample == null)
			stepSample = stepSamples.get(stepSamples.size() -1);
		int current = stepSamples.indexOf(stepSample) + 1;
		if (current >= stepSamples.size())
			current = 0;
		setSelected(current);
		MainFrame.update(this);
	}

	/** applies to all samples and stepSamples */
	public void setMix(float f) {
		this.mix = f;
		stepSamples.forEach(s->s.setMix(mix));
		this.forEach(s->s.setMix(mix));
		MainFrame.update(this);
	}
}
	
