package net.judah.gui.scope;

import java.awt.Color;
import java.awt.Dimension;

import be.tarsos.dsp.util.PitchConverter;
import net.judah.util.Constants;

/** source/inspiration:
 https://github.com/JorenSix/TarsosDSP/blob/master/examples/src/main/java/be/tarsos/dsp/example/unverified/SpectrogramPanel.java
 */
public class Spectrogram extends TimeWidget {

	private static final float S_RATE = Constants.sampleRate();
	private static final float DURATION = S_RATE / Constants.fftSize();

	public Spectrogram(Dimension size, Transform[] data) {
		super(size, data);
	}

	@Override public void analyze(int x, Transform t, int unit) {
		clearRect(x, unit); // clear for this pass
		drawX(x, t.magnitudes(), unit);
	}

	@Override void generateImage(float unit) {
		clearRect(0, w);
		int step = (int)unit;
		for (int x = 0; x < db.length; x++) {
			if (db[x] == null)
				return; // end_of_tape
			drawX(x, db[x].magnitudes(), step);
		}
	}

	private void drawX(int x, float[] amplitudes, int unit) {

		float maxAmplitude = 0f;
		int height = getHeight();
		float[] pixeledAmplitudes = new float[height];

		for (int i = 1; i < amplitudes.length - 1; i++) { // skip DC at i=0 && i=length
			double freqHz = i * DURATION; // correct bin frequency
			int pixelY = frequencyToBin(freqHz);
			if (pixelY < 0 || pixelY >= height)
				continue; // ignore out-of-range
			pixeledAmplitudes[pixelY] += amplitudes[i];
			if (pixeledAmplitudes[pixelY] > maxAmplitude)
				maxAmplitude = pixeledAmplitudes[pixelY];
		}

		//draw the pixels
		for (int y = 1; y < pixeledAmplitudes.length - 2; y++) {
			 Color color = BACKGROUND;
	         if (maxAmplitude != 0) {
	        	 final int greyValue = (int) (Math.log1p(pixeledAmplitudes[y] / maxAmplitude) / Math.log1p(1.0000001) * 255);
	         	 color = new Color(255 - greyValue, 255 - greyValue/2, 255);
	         }
	         g2d.setColor(color);
	    	 g2d.fillRect(x * unit, y, unit, 1);
		}
	}

	private int frequencyToBin(final double frequency) {
	    final double minFrequency = 40; // Hz
	    final double maxFrequency = 10000; // Hz
	    int bin = 0;
	    if (frequency != 0 && frequency > minFrequency && frequency < maxFrequency) {
            final double minCent = PitchConverter.hertzToAbsoluteCent(minFrequency);
            final double maxCent = PitchConverter.hertzToAbsoluteCent(maxFrequency);
            final double absCent = PitchConverter.hertzToAbsoluteCent(frequency);
            double binEstimate = (absCent - minCent) / (maxCent - minCent) * getHeight();
	        bin = getHeight() - 1 - (int) binEstimate;
	    }
	    return bin;
	}

}
