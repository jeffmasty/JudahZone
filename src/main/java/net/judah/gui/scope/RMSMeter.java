package net.judah.gui.scope;

import java.awt.Dimension;

import net.judah.omni.Zwing;

public class RMSMeter extends TimeWidget {

	public static final float LIVE_FACTOR = 9; // TODO live audio ~8x weaker than recorded audio?
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

	protected void drawX(int x, RMS data, int cell, boolean live) {

	    // compute pixel height from RMS and clamp to [0..baseline]
	    int height = (int) (data.rms() * rmsFactor * (live ? LIVE_FACTOR : 1));
	    int y = h - height;


	    // Color index driven by smoothed peak value
	    smoothedPeak = smoothedPeak * (1.0f - PEAK_SMOOTH) + data.peak() * PEAK_SMOOTH;
	    int colorIndex = I_SHIFT + Math.round(smoothedPeak * peaksFactor * (live ? LIVE_FACTOR : 1));
	    if (colorIndex < 0) colorIndex = 0; // defensive

	    // draw the RMS-driven bar using the rainbow color (color intensity is independent of height)
	    g2d.setColor(Zwing.chaseTheRainbow(colorIndex));
	    g2d.fillRect(x * cell, y, cell, height);
	}

	@Override public void analyze(int x, Transform t, int cell) {
		// clear play head
		clearRect(x, cell);
	    drawX(x, t.rms(), cell, true);
	}

	@Override public void generateImage(float unit) {
		clearRect(0, w);
		int step = (int)unit;
		for (int x = 0; x < db.length; x++) {
			if (db[x] == null)
				return; // end_of_tape
			drawX(x * step, db[x].rms(), step, false);
		}
	}

	public void both(int val, int unit) {
		float change = val * 0.01f;
		setIntensity(change, 0);
		setYScale(change, unit);
	}


	private void updateFactors() {
	    rmsFactor = yScale * Y_FACTOR;
	    peaksFactor = iScale * INTENSITY;
	}

	public void setIntensity(float val, int unit) {
		if (val < 0 || val > 1)
			return;
		iScale = val;
		updateFactors();
		if (unit > 0)
			generateImage(unit);
	}

	public void setYScale(float val, int unit) {
		if (val < 0 || val > 1)
			return;
		yScale = val;
		updateFactors();
		if (unit > 0)
			generateImage(unit);
	}

	public void attenuate(boolean up, int unit) {
		float val = yScale * (up ? 1.1f : 0.9f);
		if (val <= 0) val = 0.01f;
		else if (val > 1) val = 1f;
		setYScale(val, unit);
	}

}
