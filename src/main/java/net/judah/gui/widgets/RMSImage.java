package net.judah.gui.widgets;

import static judahzone.util.AudioMetrics.INTENSITY;
import static judahzone.util.AudioMetrics.I_SHIFT;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import judahzone.gui.Pastels;
import judahzone.util.AudioMetrics.RMS;
import judahzone.util.Rainbow;
import judahzone.util.WavConstants;

/** WaveImage subclass for live/offline RMS visualization with zoom & knob control. */
public class RMSImage extends WaveImage {

	private final List<RMS> amplitudes = new LinkedList<>();
	protected float yScale = 0.5f;
	protected float iScale = 0.5f;
	private String lambda;
	private int lambdaLoc;
	private int baseline;

	public RMSImage(Dimension size) {
		super(size);
		baseline = (int) (height * 0.9f);
		lambda();
	}

	/** Populate recording with RMS[] for offline visualization. */
	public void setRMS(RMS[] rmsData) {
	    synchronized (amplitudes) {
	        amplitudes.clear();
	        for (RMS r : rmsData)
	            amplitudes.add(r);
	    }
	    audioLength = rmsData.length * JACK_BUFFER;
	    fullRange(); // This sets start, end, and samplesPerPixel for full view
	    generateImage();
	}

	/** Generate RMS visualization image from current amplitudes snapshot. */
	public void generateImage() {
	    List<RMS> snapshot;
	    synchronized (amplitudes) {
	        if (amplitudes.isEmpty())
	            return;
	        snapshot = new ArrayList<>(amplitudes);
	    }

	    g2d.setColor(BACKGROUND);
	    g2d.fillRect(0, 0, width, height);

	    g2d.setColor(Pastels.RED);
	    g2d.drawLine(0, centerY, width, centerY);

	    int pixel = 0;
	    long sample = start;
	    int spp = samplesPerPixel; // Use a local copy

	    final float yFactor = yScale * WavConstants.LIVE_FACTOR * INTENSITY;
	    final float iLength = iScale * WavConstants.LIVE_FACTOR * INTENSITY;

	    for (; pixel < width; pixel++) {
	        int rmsIndex = (int) (sample / JACK_BUFFER);
	        if (rmsIndex < 0) rmsIndex = 0;
	        if (rmsIndex >= snapshot.size()) rmsIndex = snapshot.size() - 1;

	        RMS rms = snapshot.get(rmsIndex);
	        drawX(pixel, rms, yFactor, iLength, I_SHIFT + (int) (rms.rms() * yFactor));
	        sample += spp;
	    }

	    g2d.setColor(Color.DARK_GRAY);
	    g2d.drawString(lambda, lambdaLoc, baseline);
	}

	private void drawX(int x, RMS data, float yFactor, float iLength, int rmsVal) {
		g2d.setColor(Pastels.BLUE);
		g2d.drawLine(x, centerY + (int) (data.peak() * yFactor),
				x, centerY - (int) (data.peak() * yFactor));

		g2d.setColor(Rainbow.get(rmsVal));
		g2d.drawLine(x, centerY + (int) (data.amplitude() * iLength),
				x, centerY - (int) (data.amplitude() * iLength));
	}

	/** Knob idx 6 = yScale, 7 = iScale. */
	public boolean doKnob(int idx, int value) {
		float floater = value * 0.01f;
		switch (idx) {
		case 6 -> { setYScale(floater); return true; }
		case 7 -> { setIntensity(floater); return true; }
		default -> { return false; }
		}
	}

	public void setIntensity(float val) {
		if (val < 0 || val > 1) return;
		iScale = val;
		lambda();
		generateImage();
	}

	public void setYScale(float val) {
		if (val < 0 || val > 1) return;
		yScale = val;
		lambda();
		generateImage();
	}

	private void lambda() {
		int yλ = (int) Math.ceil(yScale * 100);
		int iλ = (int) Math.ceil(iScale * 100);
		lambda = "yλ:" + yλ + " iλ:" + iλ;
		int lambdaWidth = g2d.getFontMetrics().stringWidth(lambda);
		lambdaLoc = width - lambdaWidth - 8;
	}
}
