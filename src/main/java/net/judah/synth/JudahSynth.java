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
import net.judah.effects.CutFilter;
import net.judah.midi.MidiPort;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.Icons;
import net.judah.util.RTLogger;

@Getter // Wishlist: detune/note bend/portamento/glide, LFOs, Shapes: PWM,Noise, mono-synth
public class JudahSynth extends LineIn implements MidiReceiver, Engine {
	
	public static final float TUNING = 440;
	public static final int POLYPHONY = 16;
	public static final int DCO_COUNT = 3;
	public static final String ENVELOPE = "Envelope";
	public static final String FILTER = "Filter";
	public static final String DCO = "Dco";
	private static final boolean MONO = false;
	
	
    private final FloatBuffer mono = FloatBuffer.allocate(Constants.bufSize());
	private final FloatBuffer[] buffer = new FloatBuffer[] {mono, null}; // synth is not stereo (yet)
	private final int channel = 0;
	
	protected boolean active = true;
    private final Adsr adsr = new Adsr();
    private final Voice[] voices = new Voice[POLYPHONY];
    private final Polyphony notes = new Polyphony(voices);
    private final float[] dcoGain = new float[DCO_COUNT];
    private final Shape[] shapes = new Shape[] {Shape.SIN, Shape.TRI, Shape.SAW};
	private final CutFilter loCut = new CutFilter(MONO);
	private SynthPresets presets;
    @Setter @Getter private MidiPort midiPort;
    @Setter private float amplification = 0.5f;
    
	public JudahSynth(int chooChoo, JackPort left, JackPort right, String iconName) {
		super("Synth" + chooChoo, false);
		leftPort = left;
		rightPort = right;
		midiPort = new MidiPort(this);
		setIcon(Icons.load(iconName));		
		
		for (int i = 0; i < dcoGain.length; i++)
			dcoGain[i] = 0.50f;
		
		for (int i = 0; i < voices.length; i++)
			voices[i] = new Voice(i, this);

		hiCut.setFilterType(CutFilter.Type.LP24);
		hiCut.setFrequency(6000);
		hiCut.setResonance(3);
		hiCut.setActive(true);
		
		loCut.setFilterType(CutFilter.Type.HP24);
		loCut.setFrequency(60);
		loCut.setResonance(2);
		loCut.setActive(true);

		presets = new SynthPresets(this);
	}


	public SynthPresets getPresets() {
		if (presets == null)
			presets = new SynthPresets(this);
		return presets;
	}
	
	public static float midiToHz(int data1) {
        return (float)(Math.pow(2, (data1 - 57d) / 12d)) * TUNING;
    }

	public float computeGain(int dco) { // dampen
		return dcoGain[dco] * 0.1f;
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
		// pitch bend/resonance/cc
	}

	@Override
	public boolean hasWork() {
		return (active && !onMute); //&& !notes.isEmpty() -- wouldn't account for reverb/delay transients */
	}

	@Override
	public void progChange(String preset) {
		getPresets().load(new File(Constants.SYNTH, preset));
	}
	
	@Override
	public void progChange(String preset, int bank) {
		progChange(preset); // banks/channels not implemented
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

	@Override
	public String[] getPatches() {
		return presets.toArray(new String[presets.size()]);
	}


	@Override
	public int getProg(int ch) {
		return presets.getProg();
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

