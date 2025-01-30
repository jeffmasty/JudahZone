package net.judah.gui.waves;


import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;

import net.judah.omni.AudioTools;
import net.judah.omni.Zwing;

/** Recording has fixed length */
public class FileWave extends WaveImage {
	// public static final int AMPLIFY_FACTOR = 2;

	public FileWave(Dimension size, File f) throws IOException {
		super(size, new RMSRecording(f));
	}

	public FileWave(Dimension size, WaveImage waveform) {
		super(size, waveform.getRecording());
	}

	@Override
	public void generateImage() {
		if (recording.isEmpty())
			return;

		Graphics2D g2d = createGraphics();
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, width, height);

        float yFactor = yScale * AMPLITUDE;
        float iFactor = iScale * I_FACTOR;
        float iLength = iScale * AMPLITUDE;

        for (int pixel = 0; pixel < width; pixel++) { // draw peaks + RMS
        	drawX(pixel, g2d, yFactor, iFactor, iLength);
        }
        if (printabels)
        	printLabels(g2d);

        g2d.dispose();
	}

	private void drawX(int pixel, Graphics2D g2d, float yFactor, float iFactor, float iLength) {
    	recording.getSamples(start + pixel * samplesPerPixel, workarea);
    	Buffer data = analyze(workarea);
    	g2d.setColor(PEAKS);
        g2d.drawLine( pixel, (int)(centerY + data.peak().hi() * yFactor),
        			  pixel, (int)(centerY + data.peak().lo() * yFactor));

        int leftFrame = (int) ((start + (samplesPerPixel * pixel)) / (float)BUF_SIZE);
        double rms = recording.getRms(leftFrame).left();
        g2d.setColor(Zwing.chaseTheRainbow(I_SHIFT + (int) (rms * iFactor)));
    	g2d.drawLine( pixel, (int)(centerY + data.amplitude().hi() * iLength),
        			  pixel, (int)(centerY + data.amplitude().lo() * iLength));
	}

    /** print to the graphics data of x (time and frame) and y scaling. */
	@Override
	protected void printLabels(Graphics2D g) {
        int banner = 2 * MARGIN;
        int baseline = (int) (height * 0.95f);
        g.setColor(labelColor);

        // top left/right time
        g.drawString(String.valueOf(AudioTools.sampleToSeconds(start)), INSET, banner);
        String finito = AudioTools.sampleToSeconds(end);
        g.drawString(finito, width - MARGIN - g.getFontMetrics().stringWidth(finito), banner);
        // bottom right: y scalings
        int yλ = (int) Math.ceil(yScale * 100);
        int iλ = (int) Math.ceil(iScale * 100);
        String lambda = "yλ:" + yλ + " iλ:" + iλ;
        int lambdaWidth = g.getFontMetrics().stringWidth(lambda);
        g.drawString(lambda, width - MARGIN - lambdaWidth, baseline);
        // bottom left: zoom% of total Sec.
        String xλ = String.format("%s of %.2f", getZoom(), recording.seconds());
        g.drawString(xλ, INSET, baseline);
		// xFrame/totalFrame
		//String frame = ((int) (start * BUF_INVERSE)) + "/" + recording.size();

	}


}
