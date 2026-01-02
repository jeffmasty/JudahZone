package net.judah.synth.taco;

import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.fx.MonoFilter;
import judahzone.fx.MonoFilter.Type;
import judahzone.util.AudioTools;
import judahzone.util.Constants;
import judahzone.util.RTLogger;
import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.SynthKnobs;
import net.judah.midi.ChannelCC;
import net.judah.midi.Panic;
import net.judah.seq.automation.ControlChange;
import net.judah.seq.track.PianoTrack;

public class TacoSynth extends PianoTrack {

	private static final SynthDB synthPresets = new SynthDB();
	public static SynthDB getPresets() { return synthPresets; }

	public static final int OVERSAMPLE = 4; // anti-aliasing
	public static final int POLYPHONY = 24;
	public static final int DCO_COUNT = 3;
	public static final int ZERO_BEND = 8192;

	protected final float[] mono = new float[Constants.bufSize()];
	protected final float[] upsample = new float[Constants.bufSize() * OVERSAMPLE];
    // TODO Pan DCOs: protected final FloatBuffer stereo = FloatBuffer.wrap(new float[Constants.bufSize()]);

	@Getter private final Channel channel;
	@Getter private final Adsr adsr = new Adsr();
    private final Polyphony notes;
    @Getter private final float[] dcoGain = new float[DCO_COUNT];
    @Getter private final float[] detune = new float[DCO_COUNT];
    @Getter private final Shape[] shapes = new Shape[] {Shape.SIN, Shape.TRI, Shape.SAW};
    private final ModWheel modWheel;
    /** modwheel pitchbend semitones */
    @Getter @Setter private int modSemitones = 1;
    private final MonoFilter internalFilter = new MonoFilter(Type.HiCut, 16500, OVERSAMPLE);
    @Getter private final MonoFilter highPass = new MonoFilter(Type.LoCut, 50, 1);
	@Getter private final MonoFilter lowPass = new MonoFilter(Type.HiCut, 3600, 1);
    @Getter private final SynthKnobs knobs;
    private ChannelCC cc;

	// TODO: portamento/glide, LFOs, PWM, mono- vs. polysynth, true stereo, envelope to filter
	/** Taco stands for Track-Controlled Oscillator */
	public TacoSynth(String name, TacoTruck truck, Polyphony notes) throws InvalidMidiDataException {
		this(name, truck, truck.getTracks().size(), notes);
	}

	public TacoSynth(String name, TacoTruck truck, int ch, Polyphony notes) throws InvalidMidiDataException {
		super(name, notes, JudahZone.getInstance().getChords());
		this.channel = truck;
		this.notes = notes;
		cc = new ChannelCC(truck); // multiple against the truck

		for (int i = 0; i < notes.voices.length; i++)
			notes.voices[i] = new Voice(this);
		for (int i = 0; i < dcoGain.length; i++)
			dcoGain[i] = 0.50f;
		for (int i = 0; i < detune.length; i++)
			detune[i] = 1f;

		modWheel = new ModWheel(this, lowPass, highPass);
		knobs = new SynthKnobs(this);
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
	public void close() {
		new Panic(this);
	}

	@Override public boolean progChange(String preset) {
		if (synthPresets.apply(preset, this)) {
			state.setProgram(preset);
			MainFrame.updateTrack(Update.PROGRAM, this);
			return true;
		}
		return false;
	}

	@Override
	public String progChange(int data2) {
		if (data2 < 0 || data2 >= synthPresets.size())
			return null;

		String result = synthPresets.keys().get(data2);
		progChange(result);
		return result;
	}


	@Override public String[] getPatches() {
		List<String> result = synthPresets.keys();
		return result.toArray(new String[result.size()]);
	}

	@Override public void send(MidiMessage midi, long timeStamp) {
		if (midi instanceof MetaMessage)
			return; // TODO
		ShortMessage m = (ShortMessage)midi;
		if (Midi.isProgChange(m)) { // should have been filtered by MidiTrack
			progChange(m.getData1());
			return;
		}
		if (filterPiano(m))
			return; // Pedal/Panic filter
		if (cc.process(m))
			return; // channel cc filter
		if (ccEnv(m))
			return; // envelope cc filter

		if (Midi.isNote(midi))
			notes.receive(m);
		else if (ControlChange.MODWHEEL.matches(m))
			modWheel.dragged(m.getData2());
		else if (Midi.isPitchBend(m)) {
			float factor = bendFactor(m, modSemitones);
			for (Voice v : notes.voices)
				v.bend(factor);
		}
		else RTLogger.debug(this, "skip " + Midi.toString(m));

	}

	private boolean ccEnv(ShortMessage msg) {
		if (!Midi.isCC(msg))
			return false;
		// Envelope
		ControlChange type = ControlChange.find(msg.getData1());
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
		MainFrame.update(getKnobs());
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
	///
	///	  1. voice DCOs upsampled
	///   2. anti-alias filter
	///   3. decimate to mono
	///	  4. preset filters
	public void process() {
        AudioTools.silence(upsample);
        for (Voice voice : notes.voices)
        	voice.process(notes, adsr, upsample);
        internalFilter.process(upsample);
        decimate();

        highPass.process(mono);
        lowPass.process(mono);

	}

	private void decimate() {
		for (int i= 0; i < mono.length; i++)
			mono[i] = upsample[i * OVERSAMPLE];
	}

}
