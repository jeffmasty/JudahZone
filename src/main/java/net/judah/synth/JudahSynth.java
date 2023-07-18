package net.judah.synth;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Engine;
import net.judah.fx.Filter;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.Knobs;
import net.judah.gui.knobs.SynthKnobs;
import net.judah.gui.settable.Program;
import net.judah.midi.Midi;
import net.judah.midi.MidiPort;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;
import net.judah.util.RTLogger;

@Getter // Wishlist: portamento/glide, LFOs, PWM, mono-synth, true stereo
public class JudahSynth extends LineIn implements Engine, Knobs {
	public static final String[] NAMES = {"S.One", "S.Two"};
	public static final int POLYPHONY = 24;
	public static final int DCO_COUNT = 3;
	public static final int ZERO_BEND = 8192;
	
    private final FloatBuffer work = FloatBuffer.allocate(bufSize); // mono-synth algorithm
	
	private final int channel = 0;
	private final KnobMode knobMode = KnobMode.DCO;
	protected boolean active = true;
    private final Adsr adsr = new Adsr();
    private final Voice[] voices = new Voice[POLYPHONY];
    private final Polyphony notes = new Polyphony(voices);
    private final float[] dcoGain = new float[DCO_COUNT];
    private final float[] detune = new float[DCO_COUNT];
    private final Shape[] shapes = new Shape[] {Shape.SIN, Shape.TRI, Shape.SAW};
	private final ModWheel modWheel;
	private boolean mono = false; // TODO monosynth
	private final Filter loCut = new Filter(false);
	private final Filter hiCut = new Filter(false);
	
	private SynthPresets synthPresets;
	private final List<Integer> actives = new ArrayList<>();
	@Setter @Getter private MidiPort midiPort;
    /** modwheel pitchbend semitones */
    @Setter private int modSemitones = 1;
    private final SynthKnobs synthKnobs;
    
	public JudahSynth(String name, JackPort left, JackPort right, String iconName) {
		super(name, false);
		leftPort = left;
		rightPort = right;
		midiPort = new MidiPort(this);
		factor = 1.33f;
		icon = Icons.get(iconName);		
		
		for (int i = 0; i < dcoGain.length; i++)
			dcoGain[i] = 0.50f;
		for (int i = 0; i < voices.length; i++)
			voices[i] = new Voice(i, this);
		for (int i = 0; i < detune.length; i++)
			detune[i] = 1f;

		loCut.setFilterType(Filter.Type.LoCut);
		loCut.setFrequency(60);
		loCut.setResonance(2);
		loCut.setActive(true);

		hiCut.setFilterType(Filter.Type.HiCut);
		hiCut.set(Filter.Settings.Frequency.ordinal(), 65);
		hiCut.setResonance(2.5f);
		hiCut.setActive(true);

		synthPresets = new SynthPresets(this);
		modWheel = new ModWheel(hiCut, loCut);
		synthKnobs = new SynthKnobs(this);

	}

	public float computeGain(int dco) { 
		return dcoGain[dco] * 0.1f; // dampen
	}
	public Shape getShape(int dco) {
		return shapes[dco];
	}
	
	public void setGain(int dco, float val) {
		dcoGain[dco] = val;
	}
	public void setShape(int dco, Shape change) {
		shapes[dco] = change;
	}

	/** Receiver interface, this method serves as panic() */
	@Override 
	public void close() {
		notes.panic();
	}

	@Override
	public void send(MidiMessage midi, long timeStamp) {
		ShortMessage m = (ShortMessage)midi;
		if (Midi.isNote(m)) {
			if (Midi.isNoteOn(m)) { 
				if (notes.noteOn(m))
					MainFrame.update(this);
			}
			else if (notes.noteOff(m)) {
				MainFrame.update(this);
			}
		}
		else if (Midi.isProgChange(m)) {
			RTLogger.log(this, "TODO ProgChange " + new Midi(m.getMessage()).toString());
		}
		else if (Midi.isCC(m)) {
			if (m.getData1() == 1) { // modWheel
				modWheel.mod(m.getData2());
			}
		}
		else if (Midi.isPitchBend(m)) {
			float factor = bendFactor(m, modSemitones);
			for (Voice v : voices) 
				v.bend(factor);
		}
	}

	@Override
	public boolean hasWork() {
		return (active && !onMute); //&& !notes.isEmpty() -- wouldn't account for reverb/delay transients */
	}
	
	@Override public boolean isMono() { return mono; }


	@Override public boolean progChange(String preset) {
		if (getSynthPresets().load(preset)) {
			MainFrame.update(Program.first(this, 0)); 
			return true;
		}
		return false; 
	}
	
	@Override public boolean progChange(String preset, int bank) {
		return progChange(preset); // banks/channels not implemented
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

	public float detune(int dco, float hz) {
		return detune[dco] * hz;
	}

	@Override public List<Integer> getActives() {
		actives.clear();
		for (ShortMessage m : notes.getNotes()) 
			if (m != null && Midi.isNoteOn(m)) 
				actives.add(m.getData1());
		return actives;
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
	public void process() {
		
		if (!hasWork()) 
			return;
        AudioTools.silence(work);

        for (Voice voice : voices) {
        	voice.process(notes, adsr, work);
        }
        
        
        loCut.process(work);
        hiCut.process(work);
        processFx(work);
        toStereo(work);
        AudioTools.mix(left, leftPort.getFloatBuffer());
        AudioTools.mix(right, rightPort.getFloatBuffer());

	}

}

//	public void setActive(boolean on) {  add/remove synth from channels list
//		active = on;
//		new Thread(()->{
//			if (active) {
//				JudahZone.getMixer().addChannel(JudahSynth.this);
//				JudahZone.getSynthPorts().add(midiPort);
//				// JudahZone.getTracker().updateMidiPorts();
//			}
//			else {
//				notes.flush();
//				JudahZone.getMixer().removeChannel(JudahSynth.this);
//				JudahZone.getSynthPorts().remove(midiPort);
//				// JudahZone.getTracker().updateMidiPorts();
//		}}).start();}

