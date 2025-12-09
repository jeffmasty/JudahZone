package net.judah.sampler;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.PlayAudio.Type;
import net.judah.fx.Fader;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.SampleKnobs;
import net.judah.mixer.LineIn;
import net.judah.omni.AudioTools;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@Getter
public class Sampler extends LineIn {

	private ArrayList<Sample> samples = new ArrayList<Sample>();
	private final KnobMode knobMode = KnobMode.Sample;

	// Fountain Creek: Fields Park, Manitou Springs, CO
	// Rain in Cuba: https://freesound.org/people/kyles/sounds/362077/
	// Birds: https://freesound.org/people/hargissssound/sounds/345851/
	// Bicycle https://freesound.org/people/bojan_t95/sounds/507013/
	public static final String[] LOOPS = {"Creek", "Rain", "Birds", "Bicycle"};

	// Satoshi: Pantone Color of the Year Landr pack Coy loop
	// Prrrrrr: https://freesound.org/people/Duisterwho/sounds/644104/
	// DropBass: https://freesound.org/people/qubodup/sounds/218891/
	// DJOutlaw: https://freesound.org/people/tim.kahn/sounds/94748/
	public static final String[] ONESHOTS = {"Satoshi", "Prrrrrr", "DropBass", "DJOutlaw"};

	public static final String[] STANDARD = Stream.concat(
			Arrays.stream(LOOPS), Arrays.stream(ONESHOTS)).toArray(String[]::new);


	public static final int SIZE = STANDARD.length;

	private final String[] patches = new String[] {"STANDARD"};
	@Setter @Getter float stepMix = 0.5f;

	/** selected step sample */
	@Getter private int selected;
	private final ArrayList<StepSample> stepSamples = new ArrayList<>();
	private StepSample stepSample;
	private final SampleKnobs view;

	public Sampler() {
		super(Sampler.class.getSimpleName(), Constants.STEREO);
		gain.setPreamp(0.5f);

		for (int i = 0; i < STANDARD.length; i++) {
			try {
				samples.add(new Sample(STANDARD[i], i < 4 ? Type.LOOP : Type.ONE_SHOT));
			} catch (Exception e) {
				RTLogger.warn(this, e);
			}
		}
		try {
			stepSamples.add(new StepSample("Crickets", this, 4, 12));
			stepSamples.add(new StepSample("Block", this, 4, 12));
			stepSamples.add(new StepSample("Cowbell", this, 4, 12));
			stepSamples.add(new StepSample("Clap", this, 4, 6, 12));
			stepSamples.add(new StepSample("Claves", this, 4, 10, 14));
			stepSamples.add(new StepSample("Ride", this, 0, 4, 8, 12));
			stepSamples.add(new StepSample("Tambo", this, 0, 2, 4, 6, 8, 10, 12, 14));
			stepSamples.add(new StepSample("Shaker", this, 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15));
			stepSamples.add(new StepSample("Disco", this, 2, 6, 10, 14));
			stepSamples.add(new StepSample("Hats16", this, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
			stepSamples.add(new StepSample("Snares", this, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
			stepSamples.add(new StepSample("4x4", this, 0, 4, 8, 12));
			stepSample = stepSamples.get(0);
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
		view = new SampleKnobs(this);
	}

	public void play(Sample s, boolean on) {
		if (on) {
			if (MainFrame.getKnobMode() != KnobMode.Sample)
				MainFrame.setFocus(KnobMode.Sample);
			if (s.getType() == Type.ONE_SHOT)
				s.rewind();
			else
				Fader.execute(Fader.fadeIn(s));
			s.play(true);
		}
		else {
			if (s.getType() == Type.ONE_SHOT)
				s.play(false);
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

	@Override
	public void process(FloatBuffer outLeft, FloatBuffer outRight) {
		AudioTools.silence(left);
		AudioTools.silence(right);
    	for (Sample sample : samples)
    		sample.process(left, right);
    	for (StepSample s : stepSamples)
    		if (s.isOn()) {
    			s.env = stepMix;
    			s.process(left, right);
    		}

    	fx();

		AudioTools.mix(left, outLeft);
		AudioTools.mix(right, outRight);
	}

}

