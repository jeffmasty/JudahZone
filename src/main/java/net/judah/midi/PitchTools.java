package net.judah.midi;

/**
 * Small utility helpers for MIDI <-> Hz conversions using the common
 * reference A4 = MIDI 69 = 440.0 Hz.
 *
 * All primary conversion methods return floats (single precision).
 * Equal-tempered 12-tone temperament is assumed.
 */
public final class PitchTools {

    private PitchTools() { /* no instances */ }

    /** Standard reference: A4 = MIDI 69 = 440.0 Hz */
    public static final int REFERENCE_MIDI = 69;
    public static final float REFERENCE_FREQUENCY = 440.0f;

    /**Convert MIDI note number to frequency in Hz using standard reference (69 = 440 Hz).
     * Returns a float for single-precision usage.*/
    public static float midiToHz(int midi) {
        return midiToHz(midi, REFERENCE_MIDI, REFERENCE_FREQUENCY);
    }

    /**Convert MIDI note number to frequency in Hz using an explicit reference.
     * freq = referenceFreq * 2^((midi - referenceMidi) / 12)
     *
     * Returns a float. */
    public static float midiToHz(int midi, int referenceMidi, float referenceFreq) {
        double exponent = (midi - referenceMidi) / 12.0;
        double freq = referenceFreq * Math.pow(2.0, exponent);
        return (float) freq;
    }

    /**Convert frequency in Hz to fractional MIDI number using an explicit reference.
     * midi = referenceMidi + 12 * log2(frequency / referenceFreq)
     *
     * Throws IllegalArgumentException if frequency <= 0.
     * Returns a float. */
    public static float hzToMidi(float frequency, int referenceMidi, float referenceFreq) {
        if (frequency <= 0.0f) {
            throw new IllegalArgumentException("frequency must be > 0");
        }
        double ratio = frequency / referenceFreq;
        double midi = referenceMidi + 12.0 * (Math.log(ratio) / Math.log(2.0));
        return (float) midi;
    }

    /** Convert frequency in Hz rounded to the nearest integer MIDI note (standard reference). */
    public static int hzToMidi(float frequency) {
        return Math.round(hzToMidi(frequency, REFERENCE_MIDI, REFERENCE_FREQUENCY));
    }

}
