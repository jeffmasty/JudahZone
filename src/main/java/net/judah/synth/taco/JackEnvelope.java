package net.judah.synth.taco;

import lombok.RequiredArgsConstructor;

/** Performs an envelope's life cycle per Jack frame. */
@RequiredArgsConstructor
public class JackEnvelope {

    private int attack, decay, release;
    private float result;

    public void reset() {
        attack = 0;
        decay = 1000; // sustain starts after 1000 "ticks"
        release = 1000;
        result = 0f;
    }

    public float calcEnv(boolean pressed) {
        if (pressed) {
            if (attack < 1000)
                result = ++attack / 1000f;
            else if (decay > 0)
                result = (--decay / 1000f) * 0.5f + 0.5f;
            else
                result = 0.5f; // sustain level
            return result;
        } else {
            if (release > 0) {
                float relFactor = --release / 1000f;
                return relFactor * result;
            }
            return 0f;
        }
    }
}
