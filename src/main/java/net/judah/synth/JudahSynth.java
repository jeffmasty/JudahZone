package net.judah.synth;
import java.io.File;
import java.nio.FloatBuffer;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.MainFrame;
import net.judah.api.Engine;
import net.judah.api.Midi;
import net.judah.effects.CutFilter;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.Icons;

@Getter // Wishlist: detune/note bend/portamento/glide, LFOs, Shapes: PWM,Noise, mono-synth
public class JudahSynth extends LineIn implements Receiver, Engine {
	
	public static final float TUNING = 440;
	public static final int POLYPHONY = 8;
	public static final int DCO_COUNT = 3;
	public static final String ENVELOPE = "Envelope";
	public static final String FILTER = "Filter";
	public static final String DCO = "Dco";
	private static final boolean MONO = false;
	
    private final FloatBuffer mono = FloatBuffer.allocate(Constants.bufSize());
	private final FloatBuffer[] buffer = new FloatBuffer[] {mono, null}; // synth is not stereo (yet)

	protected boolean active;
	/** grab midi controller input */
	@Setter private boolean MPK; 
    private final Adsr adsr = new Adsr();
    private final Voice[] voices = new Voice[POLYPHONY];
    private final Polyphony notes = new Polyphony(voices);
    private final float[] dcoGain = new float[DCO_COUNT];
    private final Shape[] shapes = new Shape[] {Shape.SIN, Shape.TRI, Shape.SAW};
	private final CutFilter loCut = new CutFilter(MONO);
	private final CutFilter hiCut = new CutFilter(MONO);
	private SynthPresets presets;
	private SynthView view;
    private final int chooChoo;
    
	public JudahSynth(int chooChoo, JackPort left, JackPort right, String iconName) {
		super("Zone " + chooChoo, false);
		leftPort = left;
		rightPort = right;
		this.chooChoo = chooChoo; // engine count
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

		this.presets = new SynthPresets(this);
		setActive(true);
	}

	public void setActive(boolean on) {
		active = on;
		if (!active)
			notes.flush();
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

	public SynthView getView() {
		if (view == null)
			view = new SynthView(this);
		return view;
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
	}

	public void synthKnobs(int idx, int data2) {
		// preset, volume, hi-cut hz, lo-cut res, adsr
		switch (idx) {
		case 0: new Thread(() -> getView().getPresets().setSelectedIndex(
					Constants.ratio(data2, getView().getPresets().getItemCount() - 1))).start();
			break;
		case 1:
			gain.setVol(data2);
			break;
		case 2:
			hiCut.setFrequency(CutFilter.knobToFrequency(data2));
			break;
		case 3:
			loCut.setResonance(data2 * 0.25f);
			break;
		case 4:
			adsr.setAttackTime(data2 * 2);
			break;
		case 5:
			adsr.setDecayTime(data2);
			break;
		case 6:
			adsr.setSustainGain(data2 * 0.01f);
			break;
		case 7:
			adsr.setReleaseTime(data2 * 10);
			break;
		}
		MainFrame.update(this);
	}

	@Override
	public boolean hasWork() {
		return (active && !onMute); //&& !notes.isEmpty() -- wouldn't account for reverb/delay transients */
	}

	@Override
	public void progChange(String preset) {
		getPresets().load(new File(Constants.SYNTH, preset));
	}

	/////////////////////////////////
	//     PROCESS AUDIO           //
	/////////////////////////////////
	public void process() {
		
		if (!hasWork()) 
			return;
        AudioTools.silence(mono);

        for (Voice voice : voices) {
        	voice.process(notes, adsr, mono);
        }
        hiCut.process(mono);
        loCut.process(mono);
        processFx(mono);
	}


}
