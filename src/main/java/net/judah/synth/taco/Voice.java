package net.judah.synth.taco;

import judahzone.api.Frequency;
import judahzone.data.Stamp;

/** An individual voice managing three oscillators and an envelope. */
public class Voice {
    public static final int OSCILLATORS = 3;

    private final TacoSynth synth;
    private volatile int data1 = 0;  // RT-read: MIDI note
    private volatile boolean allocated = false;  // RT-read: allocation flag
    private float velocity;
    private final Dco[] oscillators = new Dco[OSCILLATORS];
    private final JackEnvelope env = new JackEnvelope();

    public Voice(TacoSynth synth) {
        this.synth = synth;
        for (int i = 0; i < OSCILLATORS; i++)
            oscillators[i] = new Dco(i, synth);
    }

    public void reset(int data1, int velocity) {
        this.data1 = data1;
        this.velocity = velocity / 127f;
        this.allocated = true;
        env.reset();
        float hz = Frequency.midiToHz(data1);
        for (Dco osc : oscillators)
            osc.setHz(hz);
    }

    public boolean isAllocated() { return allocated; }
    public int getData1() { return data1; }

    /** RT-safe voice processing. */
    public void process(Polyphony notes, Stamp adsr, float[] output) {
        if (!allocated) return;

        boolean pressed = notes.isNoteOn(data1);
        float amplify = env.calcEnv(pressed);

        if (amplify <= 0f) {
            allocated = false;
            return;
        }

        float totalGain = amplify * velocity;
        float[] wave;
        for (int i = 0; i < OSCILLATORS; i++) {
            wave = synth.getShape(i).getWave();
            oscillators[i].process(totalGain * synth.computeGain(i), wave, output);
        }
    }

    public void bend(float factor) {
        for (Dco osc : oscillators)
            osc.setBend(factor);
    }

    public void detune() {
        oscillators[0].detune();
    }
}
