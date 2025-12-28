package net.judah.gui.scope;

import java.awt.Dimension;

import net.judah.gui.widgets.RainbowFader;

public class RMSMeter extends TimeWidget {

	public static final float LIVE_FACTOR = 8; // TODO live audio ~8x weaker than recorded audio?
	public static final int Y_FACTOR = 550; // boost RMS into pixel range
	public static final int INTENSITY = 200; // scale peaks into pixels
	public static final int I_SHIFT = 37; // shift intensity off blue

	private float iScale = 0.5f;
	private float yScale = 0.5f;
	private float rmsFactor = 1f;
	private float peaksFactor = 1f;
	private float smoothedPeak = 0f;
	private static final float PEAK_SMOOTH = 0.15f; // 0 = instant, 1 = frozen

	public RMSMeter(Dimension size, Transform[] data) {
		super(size, data);
		updateFactors();
	}

	private void drawX(int xOnScreen, RMS data, int cellWidth, boolean live) {

		// compute pixel height from RMS and clamp to [0..baseline]
		int height = (int) (data.rms() * rmsFactor * (live ? LIVE_FACTOR : 1));
		if (height < 0)
			height = 0;
		if (height > h)
			height = h;

		int y = h - height;

		// Color index driven by smoothed peak value
		smoothedPeak = smoothedPeak * (1.0f - PEAK_SMOOTH) + data.peak() * PEAK_SMOOTH;
		int colorIndex = I_SHIFT + Math.round(smoothedPeak * peaksFactor * (live ? LIVE_FACTOR : 1));
		if (colorIndex < 0)
			colorIndex = 0; // defensive

		// draw the RMS-driven bar using the rainbow color (color intensity is independent of height)
		g2d.setColor(RainbowFader.chaseTheRainbow(colorIndex));
		g2d.fillRect(xOnScreen, y, cellWidth, height);
	}

	@Override
	public void analyze(int xOnScreen, Transform t, int cellWidth) {
		// clear play head for this cell
		clearRect(xOnScreen, cellWidth);
		if (t == null)
			return;
		drawX(xOnScreen, t.rms(), cellWidth, true);
	}

	@Override
	public void generateImage(float unit, int startIndex, int endIndex) {
		clearRect(0, w);
		if (startIndex < 0 || endIndex < 0 || startIndex >= db.length)
			return;
		if (endIndex >= db.length)
			endIndex = db.length - 1;

		int visibleCount = endIndex - startIndex + 1;
		if (visibleCount <= 0)
			return;

		// unit can be fractional, but we need integer cell positions
		for (int i = 0; i < visibleCount; i++) {
			int dbIndex = startIndex + i;
			Transform t = dbIndex >= 0 && dbIndex < db.length ? db[dbIndex] : null;
			if (t == null)
				continue; // holes in db for live mode / partial files

			int xOnScreen = Math.round(i * unit);
			int nextX = Math.round((i + 1) * unit);
			int cellWidth = Math.max(1, nextX - xOnScreen);
			drawX(xOnScreen, t.rms(), cellWidth, false);
		}
		drawBorder();
	}

	public void both(int val, int unitIgnored) {
		float change = val * 0.01f;
		setIntensity(change, 0);
		setYScale(change, 0);
	}

	private void updateFactors() {
		rmsFactor = yScale * Y_FACTOR;
		peaksFactor = iScale * INTENSITY;
	}

	public void setIntensity(float val, int unitIgnored) {
		if (val < 0 || val > 1)
			return;
		iScale = val;
		updateFactors();
	}

	public void setYScale(float val, int unitIgnored) {
		if (val < 0 || val > 1)
			return;
		yScale = val;
		updateFactors();
	}

	public void attenuate(boolean up, int unitIgnored) {
		float val = yScale * (up ? 1.1f : 0.9f);
		if (val <= 0)
			val = 0.01f;
		else if (val > 1)
			val = 1f;
		setYScale(val, 0);
	}
}