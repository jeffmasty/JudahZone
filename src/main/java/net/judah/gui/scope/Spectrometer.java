package net.judah.gui.scope;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;

import net.judah.gui.Detached.Floating;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** Spectrometer - FFT-based bar display with:
 *  - log-horizontal frequency axis
 *  - sensitivity slider (hides weak components)
 *  - y-scale slider + RMS level-following (height follows absolute gain)
 *  - tilt checkbox compensates higher frequencies by TILT_RANGE_DB
 */
public class Spectrometer extends JPanel implements Closeable, Floating, Gui.Mouser {

    /* Frequency range and display settings
     * TODO user adjustable */
    static final int minFreq = 55;
    static final int maxFreq = 14080;

    private static final float S_RATE = Constants.sampleRate();
    private static final int FFT_SIZE = Constants.fftSize();
    private static final float DURATION = FFT_SIZE / S_RATE;

    /* dB and numeric constants */
    static final float DB_FLOOR = -120f;
    static final float DISPLAY_RANGE_DB = 60f;    // full dynamic range shown at max sensitivity
    static final float MIN_VISIBLE_RANGE_DB = 6f; // minimum window at strictest sensitivity
    static final double EPS = 1e-12;
    // Tilt parameters: total dB boost applied from minFreq -> maxFreq when tilt is ON. (6 to 18)
    private static final float TILT_RANGE_DB = 18f;

    /* Level-following (to make Y-axis respond to absolute gain) */
    private float smoothedFrameDb = -90f;   // initial smoothed RMS dB
    private final float levelAttack = 0.25f;   // faster follow when level rises (0..1)
    private final float levelRelease = 0.92f;  // slower decay when level falls (closer to 1 = slower)
    private float referenceDb = -20f;          // frame dB that maps to unity height (user-adjustable)

    /* Graphics / buffers */
    private BufferedImage img;
    private Graphics2D g2d;

    private Transform cache;
    private final JToggleButton live;
    private final JSlider dampen = new JSlider(0, 100, 50); // sensitivity: 0 (strict) .. 100 (show many)
	private final JSlider ySlider= new JSlider(1, 100, 50);  // height multiplier control (logarithmic)
    private final JCheckBox tilt = new JCheckBox("Tilt", false);
	private int yScale = 50;
    private int sensitivity = 50;


    public Spectrometer(Dimension sz, JToggleButton live) {
        super(true);
        this.live = live;
        resized(sz.width, sz.height);
        addMouseListener(this);
        addMouseWheelListener(this);
        tilt.setToolTipText("Compensate higher frequencies");

    }

	public JComponent getControls() {
		tilt.addActionListener(l-> {if (live.isSelected() == false) repaint();});
    	dampen.addChangeListener(l->setSensitivity(dampen.getValue()));
    	ySlider.addChangeListener(l->setYScale(ySlider.getValue()));
        return Gui.box(new JLabel(" Scale "), Gui.resize(ySlider, Size.SMALLER),
        		tilt, new JLabel(" Wet "), Gui.resize(dampen, Size.SMALLER));
    }

	public void analyze(Transform t) {
		drawImage(t);
		repaint();
		cache = t;
    }

	public void clear() {
		clear(getWidth(), getHeight());
	}
	void clear(int w, int h) {
    	g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, w, h);
	}

    /**Draw frame using FFT modulus amplitudes and RMS.
     * Horizontal axis is log-scaled between minFreq .. displayMaxFreq. */
    private void drawImage(Transform t) {
        int w = getWidth();
        int h = getHeight();
    	clear(w, h);
    	float[] amplitudes = t.magnitudes();
        if (amplitudes == null || amplitudes.length == 0) {
            RTLogger.log(this, "no amplitudes");
            return;
        }

        final int binsTotal = amplitudes.length; // expected fftSize/2 or fftSize/2+1
        // compute absolute bin range used for calculations (clamp to available bins)
        int startBinCalc = (int) Math.ceil(minFreq * DURATION);
        startBinCalc = Math.max(0, Math.min(binsTotal - 1, startBinCalc));

        int endBinCalc = (int) Math.floor(maxFreq * DURATION);
        endBinCalc = Math.max(0, Math.min(binsTotal - 1, endBinCalc));

        // compute display end bin (absolute) and bins to display (clamp)
        final int binsDisplay = endBinCalc - startBinCalc + 1;

        // compute power for the full amplitude array (avoid missing upper bins)
        Powers full = computeFullPowers(amplitudes);

        // handle near-silence
        if (full.max <= EPS) {
            drawLabels(w, startBinCalc, endBinCalc);
            drawBorder();
            return;
        }

        // 2) compute frame RMS (prefer provided RMS object), and smooth for level-following
        smoothFrameDb(t.rms().rms());

        // height multiplier: yScale logarithmic slider combined with smoothed frame-level vs reference
        float logarithmic = Constants.logarithmic(yScale, 1, 2500);
        float heightMultiplier = (logarithmic * 0.1f) * (float) Math.pow(10.0, (smoothedFrameDb - referenceDb) / 20.0);
        heightMultiplier = Math.max(0f, Math.min(2f, heightMultiplier)); // clamp to avoid runaway heights

        // 3) aggregate bins into visual bars (log-horizontal axis)
        final int bars = Math.min(binsDisplay, Math.max(1, w));
        final float barWidth = Math.max(1, w / (float)bars);
        double[] barAvgPower = aggregateBarsAbsolute(full.power, startBinCalc, endBinCalc, bars);

        // If tilt compensation is requested, boost higher-frequency bars in power domain.
        // Tilt is applied before thresholding so sensitivity reflects the compensation.
        if (tilt.isSelected()) {
            // ratio and log denom reused from aggregate logic
            final double ratio = maxFreq / (double) minFreq;
            final double logDenom = Math.log(ratio);

            double maxAdjusted = 0.0;
            for (int bx = 0; bx < bars; bx++) {
                // compute fractional position for the bar center (0..1)
                double frac = (bars == 1) ? 0.0 : (bx + 0.5) / bars;
                // map to frequency (log scale)
                double freq = minFreq * Math.pow(ratio, frac);
                // normalized log position 0..1
                double normLog = Math.log(freq / minFreq) / logDenom;
                if (Double.isNaN(normLog) || normLog < 0.0) normLog = 0.0;
                if (normLog > 1.0) normLog = 1.0;

                // linear interpolation of dB boost from 0 -> TILT_RANGE_DB across the band
                double tiltDb = normLog * TILT_RANGE_DB;
                // convert dB to power multiplier (power ratio)
                double powerMul = Math.pow(10.0, tiltDb / 10.0);

                barAvgPower[bx] = barAvgPower[bx] * powerMul;
                if (barAvgPower[bx] > maxAdjusted) maxAdjusted = barAvgPower[bx];
            }
            // compute threshold based on adjusted bar powers
            float[] th = computeThreshold(maxAdjusted, sensitivity);
            float thresholdDb = th[0];
            float denom = th[1];

            // 5) render bars (color intensity independent of height multiplier)
            renderBars(barAvgPower, bars, barWidth, h, thresholdDb, denom, heightMultiplier);

        } else {
            // 4) compute sensitivity threshold from full.max (unchanged behavior)
            float[] th = computeThreshold(full.max, sensitivity);
            float thresholdDb = th[0];
            float denom = th[1];

            // 5) render bars (color intensity independent of height multiplier)
            renderBars(barAvgPower, bars, barWidth, h, thresholdDb, denom, heightMultiplier);
        }

        // draw labels and border
        drawLabels(w, startBinCalc, endBinCalc);
        drawBorder();
    }

    /**Render visual bars. Color is driven by normalizedColor (relative to threshold),
     * height is driven by normalizedColor * heightMultiplier (so height follows absolute level). */
    private void renderBars(double[] barAvgPower, int bars, float barWidth, int height,
                            float thresholdDb, float denom, float heightMultiplier) {
    	int bar = (int) Math.ceil(barWidth);

        for (int bx = 0; bx < bars; bx++) {
            double avgP = barAvgPower[bx];
            float db = (float) (10.0 * Math.log10(avgP + EPS));

            // apply sensitivity threshold
            if (db <= thresholdDb) continue;

            float normalizedColor = (db - thresholdDb) / denom;
            normalizedColor = Math.max(0f, Math.min(1f, normalizedColor));

            // height responds to absolute level via heightMultiplier
            float normalizedHeight = normalizedColor * heightMultiplier;
            normalizedHeight = Math.max(0f, Math.min(1f, normalizedHeight));

            int barHeight = (int) (normalizedHeight * height);
            int y = height - barHeight;
            int x = (int) (bx * barWidth);

            int intensity = (int) (normalizedColor * 255f);
            intensity = Math.max(0, Math.min(255, intensity));
            Color color = new Color(255 - intensity, 255 - intensity / 2, 255);

            g2d.setColor(color);
            g2d.fillRect(x, y, bar, barHeight);
        }
    }


    /** container for power[binIndex] (absolute indexing) */
    record Powers(double[] power, double max) { }

    /** Compute power for the full amplitudes array (mag^2) and return global max, no sliced-power buffers */
    private Powers computeFullPowers(float[] amplitudes) {
        double[] power = new double[amplitudes.length];
        double max = 0.0;
        for (int i = 0; i < amplitudes.length; i++) {
            float mag = amplitudes[i];
            if (!Float.isFinite(mag) || mag < 0f) mag = 0f;
            double p = mag * (double) mag;
            power[i] = p;
            if (p > max) max = p;
        }
        return new Powers(power, max);
    }

    /** Aggregate power into bars using absolute bin indices:
     * - startBinAbs .. endBinDisplayAbs are ABSOLUTE bin indices into the power[] array.
     * - For each visual bar we compute freqLeft/freqRight (log spacing) and map to absolute bin indices,
     *   clamp them, and average power across that inclusive bin range. */
    private double[] aggregateBarsAbsolute(double[] powerFull, int startBinAbs, int endBinDisplayAbs, int bars) {
        double[] out = new double[bars];
        double ratio = maxFreq / minFreq;

        for (int bx = 0; bx < bars; bx++) {
            double fracLeft = (bars == 1) ? 0.0 : (double) bx / (double) bars;
            double fracRight = (bars == 1) ? 1.0 : (double) (bx + 1) / (double) bars;
            double freqLeft = minFreq * Math.pow(ratio, fracLeft);
            double freqRight = minFreq * Math.pow(ratio, fracRight);

            // absolute bin indices
            int binLeft = (int) Math.floor(freqLeft * DURATION);
            int binRight = (int) Math.ceil(freqRight * DURATION);

            // clamp to requested display range and available array bounds
            if (binLeft < startBinAbs) binLeft = startBinAbs;
            if (binRight < startBinAbs) binRight = startBinAbs;
            if (binLeft > endBinDisplayAbs) binLeft = endBinDisplayAbs;
            if (binRight > endBinDisplayAbs) binRight = endBinDisplayAbs;
            if (binRight < binLeft) binRight = binLeft;

            double sum = 0.0;
            int count = 0;
            for (int b = binLeft; b <= binRight; b++) {
                sum += powerFull[b];
                count++;
            }
            if (count == 0) {
                // fallback to nearest single bin
                int nearest = Math.max(startBinAbs, Math.min(endBinDisplayAbs, (binLeft + binRight) / 2));
                sum = powerFull[nearest];
                count = 1;
            }
            out[bx] = sum / count;
        }
        return out;
    }

    /** Smooth RMS-derived dB into smoothedFrameDb using attack/release behavior. */
    private void smoothFrameDb(double frameRms) {
        double frameDb = 20.0 * Math.log10(frameRms + EPS);
        if (frameDb > smoothedFrameDb) {
            smoothedFrameDb = (float) (smoothedFrameDb * (1.0 - levelAttack) + frameDb * levelAttack);
        } else {
            smoothedFrameDb = (float) (smoothedFrameDb * levelRelease + frameDb * (1.0 - levelRelease));
        }
    }

    /** Compute threshold dB and denom for normalization based on slider sensitivity.
     * Returns float[]{ thresholdDb, denom }. */
    private float[] computeThreshold(double globalMaxPower, int sliderValue) {
        float s = Math.max(0f, Math.min(1f, sliderValue / 100f));
        float maxDb = (float) (10.0 * Math.log10(globalMaxPower + EPS));
        float thresholdDb = maxDb - (MIN_VISIBLE_RANGE_DB + s * (DISPLAY_RANGE_DB - MIN_VISIBLE_RANGE_DB));
        thresholdDb = Math.max(thresholdDb, DB_FLOOR);
        float denom = maxDb - thresholdDb;
        if (denom < 1e-6f) denom = 1f;
        return new float[] { thresholdDb, denom };
    }

    private void drawBorder() {
        int w = getWidth();
        int h = getHeight();
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawLine(0, 0, w, 0);
        g2d.drawLine(w - 1, 0, w - 1, h);
        g2d.drawLine(0, h - 1, w, h - 1);
        g2d.drawLine(0, 0, 0, h);
    }

    /** Draw log-spaced labels across the top between minFreq..displayMaxFreq.
     * Shows major labels and minor ticks. */
    private void drawLabels(int width, int startBin, int endBin) {
        int majorCount = 9;
        int minorTicks = 2;

        g2d.setColor(Color.BLACK);
        FontMetrics fm = g2d.getFontMetrics();

        final double ratio = (double) maxFreq / minFreq;
        final double logDenom = Math.log(ratio);

        for (int i = 0; i < majorCount; i++) {
            double majorFrac = (majorCount == 1) ? 0.0 : (double) i / (double) (majorCount - 1);
            double freqMajor = minFreq * Math.pow(ratio, majorFrac);
            double fracForX = (Math.log(freqMajor / minFreq) / logDenom);
            int xMajor = (int) Math.round(fracForX * (width - 1));

            final String label = (freqMajor >= 1000.0) ?
                    String.format("%.1fk", freqMajor / 1000.0) : String.format("%d", Math.round(freqMajor));
            int lw = fm.stringWidth(label);
            int labelX = xMajor - lw / 2;
            labelX = Math.max(0, Math.min(width - lw, labelX));
            int labelY = 2 + fm.getAscent();
            g2d.drawString(label, labelX, labelY);

            if (minorTicks > 0 && i < majorCount - 1) {
                for (int m = 1; m <= minorTicks; m++) {
                    double subFrac = (i + (double) m / (minorTicks + 1)) / (majorCount - 1);
                    double freqMinor = minFreq * Math.pow(ratio, subFrac);
                    double fracMinorForX = Math.log(freqMinor / minFreq) / logDenom;
                    int xMinor = (int) Math.round(fracMinorForX * (width - 1));
                    int minorTickHeight = 6;
                    g2d.drawLine(xMinor, 0, xMinor, minorTickHeight);
                }
            }
        }
    }

    @Override public void paint(Graphics g) {
        g.drawImage(img, 0, 0, null);
    }

    @Override public void close() throws IOException {
        g2d.dispose();
    }

    /* Optional setters for external control */
    public void setReferenceDb(float db) { this.referenceDb = db; }

    private void setYScale(int value) {
    	yScale = value;
    	updates();
    }

	private void setSensitivity(int value) {
		sensitivity = value;
		updates();
	}

	private void updates() {
		if (cache == null || live.isSelected())
			return;
		analyze(cache); // repaint
	}

	@Override public void resized(int w, int h) {
        if (g2d != null)
        	g2d.dispose();
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
		Dimension sz = new Dimension(w, h);
        Gui.resize(this, sz).setSize(sz);
	}

	@Override public void mouseWheelMoved(MouseWheelEvent e) {
		boolean up = e.getWheelRotation() < 0;
	    boolean isCtrlPressed = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK)  == InputEvent.CTRL_DOWN_MASK
	    		&& (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0;
		if (isCtrlPressed)
			dampen.setValue(dampen.getValue() + (up ? 5 : -5));
		else
			ySlider.setValue(ySlider.getValue() + (up ? 5 : -5));
	}

	@Override public void mouseClicked(MouseEvent e) {
		// guess which note user clicked..
//		float ratio = e.getX()/(float)getWidth();
//		float hz = Constants.logarithmic(ratio * 100, minFreq, maxFreq);
//		Note note = Key.toNote(hz);
//		RTLogger.log(this, String.format("%.2f", hz) + "hz " + note.full());
	}

}