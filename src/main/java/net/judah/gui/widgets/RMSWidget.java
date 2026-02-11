// java
package net.judah.gui.widgets;

import static judahzone.util.AudioMetrics.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import judahzone.gui.Pastels;
import judahzone.util.AudioMetrics.RMS;
import judahzone.util.Constants;
import judahzone.util.RTLogger;
import judahzone.util.Rainbow;
import judahzone.util.WavConstants;

// Live display starts at right edge, older frames move left
public class RMSWidget extends BufferedImage {
    static final int JACK_BUFFER = Constants.bufSize();
    static final int S_RATE = Constants.sampleRate();

    protected final int width, height, centerY, baseline;
    protected final Graphics2D g2d;
    private final List<RMS> amplitudes = new LinkedList<>(); // use LinkedList for efficient roll
    protected float yScale = 0.5f; // user adjust amplitude 0 to 1
    protected float iScale = 0.5f; // user adjust intensity 0 to 1
    protected int samplesPerPixel;
    private String lambda;
    private int lambdaLoc;

    public RMSWidget(Dimension size) {
        super(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        width = size.width;
        height = size.height;
        centerY = (int)(height * 0.5f);
        baseline = (int) (height * 0.9f);
        g2d = createGraphics();
        lambda();
    }

    /**
     * Accept a single RMS snapshot from an analyzer (audio thread).
     * Safe to be called from the audio thread.
     */
    public void accept(RMS rms) {
        try {
            synchronized (amplitudes) {
                amplitudes.add(rms);
                if (amplitudes.size() > width)
                    amplitudes.remove(0);
            }
            generateImage();
        } catch (Throwable t) {
            RTLogger.warn(this, t);
        }
    }

    public void generateImage() {
        List<RMS> snapshot;
        synchronized (amplitudes) {
            if (amplitudes.isEmpty())
                return;
            snapshot = new ArrayList<>(amplitudes);
        }

        g2d.setColor(Pastels.BG);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Pastels.RED);
        g2d.drawLine(0, centerY, width, centerY);

        int pixel = 0;
        int sample = 0;
        int frames = snapshot.size();
        if (frames < width) {
            samplesPerPixel = JACK_BUFFER;
            pixel = width - frames; // whitespace on left
        }
        else if (frames > 2 * width) { // rolling right
            samplesPerPixel = 2 * JACK_BUFFER;
            int startFrame = frames - 2 * width;
            sample = startFrame * JACK_BUFFER;
        }
        else
            samplesPerPixel = (int) ((frames * JACK_BUFFER) / (float)width);

        final float yFactor = yScale * WavConstants.LIVE_FACTOR * INTENSITY;
        final float iFactor = iScale * WavConstants.LIVE_FACTOR * Y_FACTOR;
        final float iLength = iScale * WavConstants.LIVE_FACTOR * INTENSITY;
        for (; pixel < width; pixel++) {
            int rmsIndex = (int) ((sample / (float)samplesPerPixel));
            if (rmsIndex < 0) rmsIndex = 0;
            if (rmsIndex >= snapshot.size()) rmsIndex = snapshot.size() - 1;
            RMS rms = snapshot.get(rmsIndex);
            drawX(pixel, rms, yFactor, iLength, I_SHIFT + (int) (rms.rms() * iFactor));
            sample += samplesPerPixel;
        }
        // display scaling factor:
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawString(lambda, lambdaLoc, baseline);
    }

    private void drawX(int x, RMS data, float yFactor, float iLength, int rmsVal) {
        g2d.setColor(Pastels.BLUE);
        g2d.drawLine(x, centerY + (int)(data.peak() * yFactor),
                     x, centerY - (int)(data.peak() * yFactor));

        g2d.setColor(Rainbow.get(rmsVal));
        g2d.drawLine(x, centerY + (int)(data.amplitude() * iLength),
                     x, centerY - (int)(data.amplitude() * iLength));
    }

    public void setIntensity(float val) {
        if (val < 0 || val > 1)
            return;
        iScale = val;
        lambda();
    }
    public void setYScale(float val) {
        if (val < 0 || val > 1)
            return;
        yScale = val;
        lambda();
    }

    private void lambda() {
        int yλ = (int) Math.ceil(yScale * 100);
        int iλ = (int) Math.ceil(iScale * 100);
        lambda = "yλ:" + yλ + " iλ:" + iλ;
        int lambdaWidth = g2d.getFontMetrics().stringWidth(lambda);
        lambdaLoc = width - lambdaWidth - 8 /*MARGIN*/;
    }

}
