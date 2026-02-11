package net.judah.sampler.vocoder;

import javax.sound.midi.ShortMessage;

import be.tarsos.dsp.util.fft.HannWindow;
import judahzone.data.Frequency;
import lombok.Getter;

/** Pitch-synchronous vocoder using leaky autocorrelation.
 *  Adapted from https://github.com/Stenzel/SimpleVocoder */
public class StenzelCoder extends ZoneCoder {

	// ringbuffer size (must be power of 2)
	static final int SIZE = 256;

	private int pos = 0; // write position (0-255)
	private final float[] input = new float[SIZE]; // input ringbuffer
	private final float[] output = new float[SIZE]; // output ringbuffer
	private final float[] correlation = new float[SIZE]; // autocorrelation lags
	private final float[] window = new HannWindow().generateCurve(SIZE);

	@Getter private float hz = 440; // pitch tracking (MIDI-driven)
	private float phase = 0.0f;  // wrapped phase accumulator
	private float delta = 0.0f;  // phase increment per sample

	public StenzelCoder() {
		preamp = 1.0f;
		setHz(440); // Initialize pitch to A4
	}

	private void setHz(float hz) {
		this.hz = hz;
		delta = TWO_PI * hz * ISR;
	}

	public void setHz(Frequency freq) {
		setHz(freq.hz);
	}

	public void setNote(ShortMessage noteOn) {
		setHz(noteOn == null ? 440 : Frequency.midiToHz(noteOn.getData1()));
	}

	/** Process flow (per sample):
		1. Store input sample: X[pos] = input
		2. Update autocorrelation: R[lag] = R[lag] * 0.9975 + 0.0025 * X[pos] * X[rd--]
		3. On phase wrap (pitch-sync):
		   - Calculate gain: scale = 1.0 / sqrt(R[0] + epsilon)
		   - Window and accumulate: Y[wy] += R[lag] * window[lag] * scale
		4. Output: Y[pos], clear Y[pos], advance pos
	 */
	@Override
	protected void processImpl(float[] monoIn, float[] monoOut) {
		for (int i = 0; i < monoIn.length; i++) {
			/** Store input sample */
			input[pos] = monoIn[i];

			/** Leaky autocorrelation: R[lag] = R[lag] * decay + (1-decay) * X[pos] * X[rd--] */
			int rd = pos;
			for (int lag = 0; lag < 128; lag++)
				correlation[lag] = correlation[lag] * decay + (1.0f - decay) * input[pos] * input[rd--];

			/** Pitch-synchronous operation on phase wrap */
			phase = (phase + delta) % TWO_PI;
			if (phase < delta) {
				int wy = pos;
				float scale = 1.0f / (float) Math.sqrt(correlation[0] + 1e-10f);

				/** Add backwards (R[128-k]) */
				for (int k = 1; k < 128; k++)
					output[wy++] += correlation[128 - k] * window[k] * scale;

				/** Add forwards (R[k]) */
				for (int k = 0; k < 128; k++)
					output[wy++] += correlation[k] * window[k + 128] * scale;
			}

			/** Read output sample and clear ringbuffer */
			monoOut[i] = output[pos];
			output[pos] = 0.0f;

			/** Advance write position (wrap at 256) */
			pos = (pos + 1) & 0xFF;
		}
	}

}
/*void SimpleVocoderProcessor::processBlock (juce::AudioBuffer<float>& buffer, juce::MidiBuffer& midiMessages) {
juce::ScopedNoDenormals noDenormals;
// midi handling omitted
const float *src = buffer.getReadPointer(0);              //input
float *dst = buffer.getWritePointer(0);                   //output
for(int i=0; i<buffer.getNumSamples(); i++)               // loop over samples
{
    X[pos] =  *src++;
    unsigned char rd = pos;
    for(int lag=0; lag<128; lag++)  R[lag] = R[lag] * 0.9975f + 0.0025f * X[pos] * X[rd--]; //leaky autocorrelation

    phase += delta;             // wrapped phase
    if(phase < delta)           // cheap pitch synchronous operation
    {
        unsigned char wy = pos;                          //output write position
        const float scale = 1.0f/sqrtf(R[0] + 0x1p-10f); //gain correction
        for(int k=1; k<128; k++)  Y[wy++] += R[128-k] * window[k] * scale;  //add backwards
        for(int k=0; k<128; k++)  Y[wy++] += R[k] * window[k+128] * scale;  //add forward
    }
    *dst++ = Y[pos];              //copy output sample
    Y[pos++] = 0.0f;              //clear output sample in ringbuffer
}
for (int i = 1; i < getTotalNumOutputChannels(); ++i) buffer.copyFrom(i, 0, buffer, 0, 0, buffer.getNumSamples()) ;
}*/
