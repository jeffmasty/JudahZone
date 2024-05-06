package net.judah.synth;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;
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
import net.judah.gui.knobs.SynthKnobs;
import net.judah.gui.settable.Program;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.util.AudioTools;
import net.judah.util.RTLogger;

@Getter // Wishlist: portamento/glide, LFOs, PWM, mono-synth, true stereo, envelope to filter
public class JudahSynth extends Engine {

	public static final String[][] NAMES = {{"S.One", "Synth.png"}, {"S.Two", "Waveform.png"}};
	public static final int POLYPHONY = 24;
	public static final int DCO_COUNT = 3;
	public static final int ZERO_BEND = 8192;
	
    private final FloatBuffer work = FloatBuffer.allocate(bufSize); // mono-synth algorithm
    
	private final boolean mono = false; // TODO monosynth switch
	private final int MIDI_CH = 0; // TODO channel-aware
	private final KnobMode knobMode = KnobMode.DCO;
	protected boolean active = true;
	private final Adsr adsr = new Adsr();
    private final Polyphony notes;
    private final float[] dcoGain = new float[DCO_COUNT];
    private final float[] detune = new float[DCO_COUNT];
    private final Shape[] shapes = new Shape[] {Shape.SIN, Shape.TRI, Shape.SAW};
	private final ModWheel modWheel;
	private final Filter loCut = new Filter(false);
	private final Filter hiCut = new Filter(false);
	
	private SynthPresets synthPresets;
    /** modwheel pitchbend semitones */
    @Setter private int modSemitones = 1;
    private final SynthKnobs synthKnobs;
    
    public JudahSynth(int idx, JackPort left, JackPort right, JudahClock clock) {
		super(NAMES[idx][0], false);
		icon = Icons.get(NAMES[idx][1]);
		leftPort = left;
		rightPort = right;
				
		for (int i = 0; i < dcoGain.length; i++)
			dcoGain[i] = 0.50f;
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
		notes = new Polyphony(this, MIDI_CH, POLYPHONY);
		try {
			tracks.add(new PianoTrack(name, notes, clock));
		} catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }
		synthKnobs = new SynthKnobs(this);
		setPreamp(1.8f);
    }

    @Override
	public Vector<MidiTrack> getTracks() {
    	return tracks;
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
		if (Midi.isNote(midi)) 
			notes.receive(m);
		else if (Midi.isProgChange(m)) {
			RTLogger.log(this, "TODO ProgChange " + new Midi(m.getMessage()).toString());
		}
		else if (Midi.isCC(m)) {
			if (m.getData1() == 1)  // modWheel
				modWheel.mod(m.getData2());
		}
		else if (Midi.isPitchBend(m)) {
			float factor = bendFactor(m, modSemitones);
			for (Voice v : notes.voices) 
				v.bend(factor);
		}
	}

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
		
		if (!active || onMute) 
			return;
        AudioTools.silence(work);

        for (Voice voice : notes.voices) {
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

