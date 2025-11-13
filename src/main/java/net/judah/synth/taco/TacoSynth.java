package net.judah.synth.taco;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.swing.ImageIcon;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Engine;
import net.judah.fx.Filter;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.SynthKnobs;
import net.judah.gui.settable.Program;
import net.judah.midi.ChannelCC;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.omni.AudioTools;
import net.judah.seq.MidiConstants.CC;
import net.judah.seq.Trax;
import net.judah.seq.track.PianoTrack;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

// TODO: up/down sample, portamento/glide, LFOs, PWM, mono- vs. polysynth, true stereo, envelope to filter
/** Taco stands for Track-Controlled Oscillator */
@Getter public final class TacoSynth extends Engine {

	public static final int POLYPHONY = 24;
	public static final int DCO_COUNT = 3;
	public static final int ZERO_BEND = 8192;

	private final Vector<PianoTrack> tracks = new Vector<PianoTrack>();
	private final KnobMode knobMode = KnobMode.Taco;
	private final Adsr adsr = new Adsr();
    private final Polyphony notes;
    private final float[] dcoGain = new float[DCO_COUNT];
    private final float[] detune = new float[DCO_COUNT];
    private final Shape[] shapes = new Shape[] {Shape.SIN, Shape.TRI, Shape.SAW};
	private ChannelCC cc = new ChannelCC(this);
    private final ModWheel modWheel;
    /** modwheel pitchbend semitones */
    @Setter private int modSemitones = 1;
    private final Filter internalFilter = new Filter(false);
	private final Filter loCut = new Filter(false);
	private final Filter hiCut = new Filter(false);
	private final SynthPresets synthPresets = new SynthPresets(this);
    private final SynthKnobs knobs;
	// private final boolean mono = false; // TODO mono-poly switch
	// private final int MIDI_CH = 0;  // TODO channel-aware

    public TacoSynth(String name, ImageIcon picture, JudahClock clock) {
    	super(name, Constants.MONO);
    	icon = picture;

		for (int i = 0; i < dcoGain.length; i++)
			dcoGain[i] = 0.50f;
		for (int i = 0; i < detune.length; i++)
			detune[i] = 1f;

		internalFilter.setFilterType(Filter.Type.HiCut);
		internalFilter.setFrequency(15500);
		internalFilter.setResonance(0f);
		internalFilter.setActive(true);

		hiCut.setFilterType(Filter.Type.HiCut);
		hiCut.setFrequency(3600);
		hiCut.setResonance(2f);
		hiCut.setActive(true);

		loCut.setFilterType(Filter.Type.LoCut);
		loCut.setFrequency(50);
		loCut.setResonance(1f);
		loCut.setActive(true);

		notes = new Polyphony(this, 0, POLYPHONY);
		knobs = new SynthKnobs(this);
		modWheel = new ModWheel(knobs, hiCut, loCut);
    }

    public TacoSynth(Trax type, ImageIcon picture, JudahClock clock) {
    	this(type.name(), picture, clock);
		try {
			tracks.add(new PianoTrack(type, notes, clock));
		} catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }
    }

    public PianoTrack getTrack() {
    	return tracks.getFirst();
    }

	public float computeGain(int dco) {
		return dcoGain[dco] * 0.1f; // dampen
	}
	public void setGain(int dco, float val) {
		dcoGain[dco] = val;
	}

	public Shape getShape(int dco) {
		return shapes[dco];
	}
	public void setShape(int dco, Shape change) {
		shapes[dco] = change;
	}

	/** Receiver interface, this method serves as panic() */
	@Override public void close() {
		new Panic(getTrack());
	}

	@Override public boolean progChange(String preset) {
		if (synthPresets.load(preset)) {
			MainFrame.update(Program.first(this, 0));
			return true;
		}
		return false;
	}

	@Override public boolean progChange(String preset, int bank) {
		return progChange(preset); // banks/channels not implemented
	}

	@Override
	public String progChange(int data2, int ch) {
		if (data2 < 0 || data2 >= JudahZone.getSynthPresets().size())
			return null;

		String result = JudahZone.getSynthPresets().keys().get(data2);
		progChange(result, ch);
		return result;
	}


	@Override public String[] getPatches() {
		List<String> result = JudahZone.getSynthPresets().keys();
		return result.toArray(new String[result.size()]);
	}

	@Override public String getProg(int ch) {
		if (synthPresets.getCurrent() == null)
			return "none";
		return synthPresets.getCurrent();
	}

	@Override public void send(MidiMessage midi, long timeStamp) {
		if (midi instanceof MetaMessage)
			return; // TODO
		ShortMessage m = (ShortMessage)midi;
		if (Midi.isProgChange(m)) { // should have been filtered by MidiTrack
			progChange(m.getData1(), m.getChannel());
			return;
		}
		if (cc.process(m))
			return; // channel cc filter
		if (ccEnv(m))
			return; // envelope cc filter

		if (Midi.isNote(midi))
			notes.receive(m);
		else if (CC.MODWHEEL.matches(m))
			modWheel.dragged(m.getData2());
		else if (Midi.isPitchBend(m)) {
			float factor = bendFactor(m, modSemitones);
			for (Voice v : notes.voices)
				v.bend(factor);
		}
		else RTLogger.debug("TacoSynth skip " + Midi.toString(m));

	}

	private boolean ccEnv(ShortMessage msg) {
		if (!Midi.isCC(msg))
			return false;
		// Envelope
		CC type = CC.find(msg.getData1());
		if (type == null)
			return false;
		int val = (int) (msg.getData2() * Constants.TO_100);

		switch(type) {
		// TODO fine-tune
			case ATTACK:
				adsr.setAttackTime(val);
				break;
			case DECAY:
				adsr.setDecayTime(val);
				break;
			case SUSTAIN:
				adsr.setSustainGain(val * Constants.TO_1);
			case RELEASE:
				adsr.setReleaseTime(val);
				break;
			case DETUNE:
				setDetune(val);
				break;
			case GLIDE: // wowser
				break;
				//case PORTAMENTO: ????
			default:
				return false;

		}
		MainFrame.update(knobs);
		return true;
	}

	public float detune(int dco, float hz) {
		return detune[dco] * hz;
	}

	/**Convert knob/CC input to floating point on DCO-0
	 * @param val 0 to 100 based around 50*/
	public void setDetune(int val) {
		detune[0] = (val - 50f) * 0.001f + 1f;
		for (Voice voice : notes.voices)
			voice.detune();
	}

	/**Pitch Bend message  https://sites.uci.edu/camp2014/2014/04/30/managing-midi-pitchbend-messages/ <pre>
	1. Combine the MSB and LSB to get a 14-bit value.
	2. Map that value (which will be in the range 0 to 16,383) to reside in the range -1 to 1.
	3. Multiply that by the number of semitones in the ± bend range.
	4. Divide that by 12 (the number of equal-tempered semitones in an octave) and use the result as the exponent of 2 to get the
        pitchbend factor (the value by which we will multiply the base frequency of the tone or the playback rate of the sample).</pre>*/
	public static float bendFactor(ShortMessage pitchMsg, int semitones) {
		int msblsb = (pitchMsg.getData2() * 128) + pitchMsg.getData1();
		float bendPercent = semitones * 2 * ((msblsb + ZERO_BEND) / (2 * (float)ZERO_BEND) - 1);
		return (float)Math.pow(2, bendPercent / 12f);
	}

	/////////////////////////////////
	//     PROCESS AUDIO           //
	/////////////////////////////////
	@Override
	public void process(FloatBuffer outLeft, FloatBuffer outRight) {

		if (onMute)
			return;
        AudioTools.silence(left);

        for (Voice voice : notes.voices)
        	voice.process(notes, adsr, left);

        internalFilter.process(left);
        loCut.process(left);
        hiCut.process(left);
        fx();
        AudioTools.mix(left, outLeft);
        AudioTools.mix(right, outRight);
	}

}

