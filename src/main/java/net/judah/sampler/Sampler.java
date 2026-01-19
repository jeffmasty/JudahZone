package net.judah.sampler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import judahzone.api.Asset;
import judahzone.api.PlayAudio;
import judahzone.api.PlayAudio.Type;
import judahzone.jnajack.BasicPlayer;
import judahzone.util.AudioTools;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
import lombok.Getter;
import lombok.Setter;
import net.judah.channel.LineIn;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.SampleKnobs;
import net.judah.mixer.Fader;

@Getter
public class Sampler extends LineIn {

    private ArrayList<Sample> samples = new ArrayList<Sample>();
    private final KnobMode knobMode = KnobMode.Sample;
    private final CopyOnWriteArrayList<BasicPlayer> players = new CopyOnWriteArrayList<>();

    public static final String[] LOOPS = SampleDB.LOOPS;
    public static final String[] ONESHOTS = SampleDB.ONESHOTS;
    public static final String[] STANDARD = Stream.concat(
            Arrays.stream(LOOPS), Arrays.stream(ONESHOTS)).toArray(String[]::new);
    public static final int SIZE = STANDARD.length;

    private final String[] patches = new String[] {"STANDARD"};
    @Setter @Getter float stepMix = 0.5f;

    @Getter private int selected;
    private final ArrayList<StepSample> stepSamples = new ArrayList<>();
    private StepSample stepSample;
    private SampleKnobs view;

    public Sampler() {
        super(Sampler.class.getSimpleName(), Constants.STEREO);
    	onMixer = false;

        if (!SampleDB.isInitialized()) {
            SampleDB.init();
            while (!SampleDB.isInitialized()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					RTLogger.warn(this, e);
				}
			}
        }
        // load(); 8 samples and x stepSamples from SampleDB
        for (int i = 0; i < STANDARD.length; i++) {
            try {
                String name = STANDARD[i];
                Asset asset = SampleDB.get(Asset.Category.SAMPLER, name);
                if (asset == null) {
                    asset = new Asset(name, new java.io.File(Folders.getSamples(), name + ".wav"), null, 0L, Asset.Category.SAMPLER);
                    SampleDB.registerAsset(asset);
                }
                samples.add(new Sample(asset, i < LOOPS.length ? Type.LOOP : Type.ONE_SHOT));
            } catch (Exception e) {
                RTLogger.warn(this, e);
            }
        }
        try {
            addStep("Crickets", 4, 12);
            addStep("Block", 4, 12);
            addStep("Cowbell", 4, 12);
            addStep("Clap", 4, 6, 12);
            addStep("Claves", 4, 10, 14);
            addStep("Ride", 0, 4, 8, 12);
            addStep("Tambo", 0, 2, 4, 6, 8, 10, 12, 14);
            addStep("Shaker", 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15);
            addStep("Disco", 2, 6, 10, 14);
            addStep("Hats16", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
            addStep("Snares", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
            addStep("4x4", 0, 4, 8, 12);

            stepSample = stepSamples.get(0);
        } catch (Exception e) {
            RTLogger.warn(this, e);
        }

    }

    public SampleKnobs getView() {
        if (view == null)
            view = new SampleKnobs(this);
        return view;
    }

    private void addStep(String name, int... steps) throws Exception {
        Asset asset = SampleDB.get(Asset.Category.STEPSAMPLE, name);
        if (asset == null) {
            // fallback: register an Asset pointing at samples folder
            asset = new Asset(name, new java.io.File(Folders.getSamples(), name + ".wav"), null, 0L, Asset.Category.STEPSAMPLE);
            SampleDB.registerAsset(asset);
        }
        stepSamples.add(new StepSample(asset, this, steps));
    }

	// add a PlayAudio for hot iteration (thread-safe)
	public BasicPlayer add(BasicPlayer player) {
		if (player == null) return null;
		players.addIfAbsent(player);
		return player;
	}

	// remove a PlayAudio
	public void remove(PlayAudio player) {
		if (player == null) return;
		players.remove(player);
	}

	// return a snapshot copy of players
	public ArrayList<BasicPlayer> getPlayers() {
		return new ArrayList<>(players);
	}

	public void playByName(String name, boolean on) {
	    if (name == null) return;
	    try {
	        // iterate existing samples (avoid allocations)
	        for (Sample s : samples) {
	            if (name.equalsIgnoreCase(s.getName())) {
	                play(s, on);
	                return;
	            }
	        }
	        RTLogger.warn(this, "playByName: sample not found: " + name);
	    } catch (Throwable t) {
	        RTLogger.warn(this, t);
	    }
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
	protected void processImpl() {
		AudioTools.silence(left);
		AudioTools.silence(right);
    	for (Sample sample : samples)
    		sample.process(left, right);
    	for (StepSample s : stepSamples)
    		if (s.isOn()) {
    			s.setEnv(stepMix);
    			s.process(left, right);
    		}

    	fx();

    	for (BasicPlayer player : players) {
    		if (player.isPlaying())
    			player.process(left, right);
    	}
	}

}
