package net.judah.synth;
import java.io.File;
import java.nio.FloatBuffer;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Engine;
import net.judah.api.Midi;
import net.judah.api.MidiReceiver;
import net.judah.controllers.KnobMode;
import net.judah.controllers.Knobs;
import net.judah.effects.CutFilter;
import net.judah.midi.MidiPort;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.Icons;
import net.judah.util.RTLogger;

@Getter // Wishlist: portamento/glide, LFOs, PWM, mono-synth
public class JudahSynth extends LineIn implements MidiReceiver, Engine, Knobs {
	
	public static final int POLYPHONY = 16;
	public static final int DCO_COUNT = 3;
	public static final int ZERO_BEND = 8192;
	
    private final FloatBuffer mono = FloatBuffer.allocate(bufSize);
	private final FloatBuffer[] buffer = new FloatBuffer[] {mono, null}; // synth is not stereo (yet)
	private final int channel = 0;
	private final KnobMode knobMode;

	protected boolean active = true;
    private final Adsr adsr = new Adsr();
    private final Voice[] voices = new Voice[POLYPHONY];
    private final Polyphony notes = new Polyphony(voices);
    private final float[] dcoGain = new float[DCO_COUNT];
    private final float[] detune = new float[DCO_COUNT];
    private final Shape[] shapes = new Shape[] {Shape.SIN, Shape.TRI, Shape.SAW};
	private final CutFilter loCut = new CutFilter(false);
	private final ModWheel modWheel;
	
	private SynthPresets synthPresets;
    @Setter @Getter private MidiPort midiPort;
    @Setter private float amplification = 0.5f;
    /** modwheel pitchbend semitones */
    @Setter private int modSemitones = 1; 
    
	public JudahSynth(int idx, JackPort left, JackPort right, String iconName) {
		super("Synth" + idx, false);
		knobMode = (idx == 1) ? KnobMode.Synth1 : KnobMode.Synth2;
		leftPort = left;
		rightPort = right;
		midiPort = new MidiPort(this);
		setIcon(Icons.load(iconName));		
		
		for (int i = 0; i < dcoGain.length; i++)
			dcoGain[i] = 0.50f;
		for (int i = 0; i < voices.length; i++)
			voices[i] = new Voice(i, this);
		for (int i = 0; i < detune.length; i++)
			detune[i] = 1f;

		hiCut.setFilterType(CutFilter.Type.LP24);
		hiCut.setFrequency(6000);
		hiCut.setResonance(3);
		hiCut.setActive(true);
		
		loCut.setFilterType(CutFilter.Type.HP24);
		loCut.setFrequency(60);
		loCut.setResonance(2);
		loCut.setActive(true);

		synthPresets = new SynthPresets(this);
		modWheel = new ModWheel(loCut, hiCut);
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
		if (Midi.isNote(m)) 
			if (Midi.isNoteOn(m)) 
				notes.noteOn(m);
			else {
				notes.noteOff(m);
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

	@Override
	public void progChange(String preset) {
		getSynthPresets().load(new File(Constants.SYNTH, preset));
	}
	
	@Override
	public void progChange(String preset, int bank) {
		progChange(preset); // banks/channels not implemented
	}

	@Override
	public String[] getPatches() {
		return synthPresets.toArray(new String[synthPresets.size()]);
	}


	@Override
	public int getProg(int ch) {
		return synthPresets.getProg();
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
	public void process() {
		
		if (!hasWork()) 
			return;
        AudioTools.silence(mono);

        for (Voice voice : voices) {
        	voice.process(notes, adsr, mono, amplification);
        }
        loCut.process(mono);
        processFx(mono);
	}

	public float detune(int dco, float hz) {
		return detune[dco] * hz;
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

