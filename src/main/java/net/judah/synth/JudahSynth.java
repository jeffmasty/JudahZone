package net.judah.synth;
import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Midi;
import net.judah.effects.CutFilter;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;
import net.judah.util.Constants;

// https://github.com/holotrash/oscillator/blob/master/oscillator.c
// https://github.com/OneLoneCoder/synth
@Getter
public class JudahSynth extends LineIn {

	public static final float TUNING = 440;
	
    @Setter protected boolean active;
    @Setter private boolean MPK;
    private final Adsr env = new Adsr();
    private final Voice[] voices = new Voice[Polyphony.MAX];
    private final Polyphony notes = new Polyphony(voices);
    private SynthView view;
	private final CutFilter lowCut = new CutFilter(false);
	private final CutFilter hiCut = new CutFilter(false);

    private final float[] mono = new float[Constants.bufSize()]; // output work buffer
    private final FloatBuffer monoBuf = FloatBuffer.wrap(mono);

    float[] dcoGain = new float[3];
    Shape[] shapes = new Shape[] {Shape.SIN, Shape.TRI, Shape.SAW};
    
    
	public JudahSynth(JackPort l, JackPort r) {
		super("SYNTH", false);
		leftPort = l;
		rightPort = r;
		gain.setVol(40);
		for (int i = 0; i < dcoGain.length; i++)
			dcoGain[i] = 0.50f;
		
		for (int i = 0; i < voices.length; i++)
			voices[i] = new Voice(i, this);

		hiCut.setFilterType(CutFilter.Type.LP24);
		hiCut.setFrequency(9500);
		hiCut.setResonance(0);
		hiCut.setActive(true);
		
		lowCut.setFilterType(CutFilter.Type.HP24);
		lowCut.setFrequency(50);
		lowCut.setResonance(0);
		lowCut.setActive(true);
		setActive(true);
	}

	public static float midiToHz(int data1) {
        return (float)(Math.pow(2, (data1 - 57d) / 12d)) * TUNING;
    }

	public float computeGain(int dco) {
		return dcoGain[dco] * 0.2f;
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
	
	public void midiProcessed(Midi m) {
		if (Midi.isNote(m)) 
			if (Midi.isNoteOn(m)) 
				notes.noteOn(m);
			else 
				notes.noteOff(m);
	}
	
	public SynthView getView() {
		if (view == null)
			view = new SynthView(this);
		return view;
	}
	
	/////////////////////////////////
	//     PROCESS AUDIO           //
	/////////////////////////////////
	@Override
	public void process() {
		if (!active) 
			return;

        AudioTools.silence(mono);
        
        for (Voice voice : voices) {
        	voice.process(notes, mono);
        }
        
        hiCut.process(monoBuf);
        lowCut.process(monoBuf);

        processFx(monoBuf);
        toJackLeft = leftPort.getFloatBuffer();
        toJackLeft.rewind();
        AudioTools.mix(mono, toJackLeft);
        
	}

	
}
