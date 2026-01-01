package net.judah.gui.widgets;

import static judahzone.util.AudioMetrics.*;

//import static net.judahzone.scope.RMSMeter.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import judahzone.api.Live;
import judahzone.gui.Pastels;
import judahzone.util.AudioMetrics;
import judahzone.util.AudioMetrics.RMS;
import judahzone.util.Constants;
import judahzone.util.Rainbow;

// Live display starts at right edge, older frames move left
public class RMSWidget extends BufferedImage implements Live {

	static final int JACK_BUFFER = Constants.bufSize();
	static final int S_RATE = Constants.sampleRate();

	protected final int width, height, centerY, baseline;
	protected final Graphics2D g2d;
	private final ArrayList<RMS> amplitudes = new ArrayList<RMS>();
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

	public void generateImage() {
		if (amplitudes.isEmpty())
			return;

	    g2d.setColor(Pastels.EGGSHELL);
	    g2d.fillRect(0, 0, width, height);

	    g2d.setColor(Pastels.RED);
	    g2d.drawLine(0, centerY, width, centerY);

	    int pixel = 0;
	    int sample = 0;
	    int frames = amplitudes.size();
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

	    final float yFactor = yScale * LIVE_FACTOR * INTENSITY;
	    final float iFactor = iScale * LIVE_FACTOR * Y_FACTOR;
	    final float iLength = iScale * LIVE_FACTOR * INTENSITY;
    	for (; pixel < width; pixel++) {
           	int rmsIndex = (int) ((sample / (float)samplesPerPixel));
            RMS rms = amplitudes.get(rmsIndex);
    		drawX(pixel, rms, yFactor, iLength, I_SHIFT + (int) (rms.rms() * iFactor));
    		sample += samplesPerPixel;
    	}
    	// display scaling factor:
	    g2d.setColor(Color.DARK_GRAY);
	    g2d.drawString(lambda, lambdaLoc, baseline);
	}

	private void drawX(int x, RMS data, float yFactor, float iLength, int rms) {
    	g2d.setColor(Pastels.BLUE);
        g2d.drawLine(x, centerY + (int)(data.peak() * yFactor),
        			 x, centerY - (int)(data.peak() * yFactor));

        g2d.setColor(Rainbow.get(rms));
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

	@Override public void analyze(float[] left, float[] right) {
		amplitudes.add(analyze(new float[][] {left, right}));
		while(amplitudes.size() > width)
			amplitudes.removeFirst(); // roll image/data left
		generateImage();
	}

	public static RMS analyze(float[] channel) {
	    float sumPositive = 0;
	    float sumNegative = 0;
	    int countPositive = 0;
	    int countNegative = 0;
	    float min = Float.MAX_VALUE;
	    float max = Float.MIN_VALUE;

	    for (float val : channel) {
	        if (val > 0) {
	            sumPositive += val;
	            countPositive++;
	        } else if (val < 0) {
	            sumNegative += val;
	            countNegative++;
	        }
	        if (val < min)
	            min = val;
	        if (val > max)
	            max = val;
	    }

	    float avgPositive = countPositive > 0 ? sumPositive / countPositive : 0;
	    float avgNegative = countNegative > 0 ? sumNegative / countNegative : 0;
	    float rms = AudioMetrics.rms(channel);
	    float peak = hiLo(max, min);
	    float amp = hiLo(avgPositive, avgNegative);
	    return new RMS(rms, peak, amp);
	}

	public static RMS analyze(float[][] in) {
		RMS left = analyze(in[0]);
		RMS right = analyze(in[1]);
		return left.rms() > right.rms() ? left : right;
	}

	private static float hiLo(float pos, float neg) {
		if (pos > Math.abs(neg))
			return pos;
		return Math.abs(neg);
	}
}


