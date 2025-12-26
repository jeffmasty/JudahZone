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

	@Override
	public void analyze(int xOnScreen, Transform t, int cellWidth) {
		if (t == null)
			return;
		// clear for this pass
		clearRect(xOnScreen, cellWidth);
		drawX(xOnScreen, t.magnitudes(), cellWidth);
	}

	@Override
	void generateImage(float unit, int startIndex, int endIndex) {
		clearRect(0, w);
		if (startIndex < 0 || endIndex < 0 || startIndex >= db.length)
			return;
		if (endIndex >= db.length)
			endIndex = db.length - 1;

		int visibleCount = endIndex - startIndex + 1;
		if (visibleCount <= 0)
			return;

		for (int i = 0; i < visibleCount; i++) {
			int dbIndex = startIndex + i;
			Transform t = dbIndex >= 0 && dbIndex < db.length ? db[dbIndex] : null;
			if (t == null)
				continue;

			int xOnScreen = Math.round(i * unit);
			int nextX = Math.round((i + 1) * unit);
			int cellWidth = Math.max(1, nextX - xOnScreen);

			drawX(xOnScreen, t.magnitudes(), cellWidth);
		}
		drawBorder();
	}

	private void drawX(int xOnScreen, float[] amplitudes, int cellWidth) {

		if (amplitudes == null || amplitudes.length == 0)
			return;

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

		// draw the pixels
		for (int y = 1; y < pixeledAmplitudes.length - 2; y++) {
			Color color = BACKGROUND;
			if (maxAmplitude != 0) {
				final int greyValue = (int) (Math.log1p(pixeledAmplitudes[y] / maxAmplitude)
						/ Math.log1p(1.0000001) * 255);
				color = new Color(255 - greyValue, 255 - greyValue / 2, 255);
			}
			g2d.setColor(color);
			g2d.fillRect(xOnScreen, y, cellWidth, 1);
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