package net.judah.gui.waves;

import static net.judah.util.Constants.LEFT;
import static net.judah.util.Constants.RIGHT;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;
import net.judah.mixer.Mains;
import net.judah.omni.AudioTools;
import net.judah.omni.Zwing;
import net.judah.util.Memory;

// Recording starts at 0 size and grows to maxFrames.
// Display starts at right edge, older frames left
public class LiveWave extends WaveImage {

	public static record LiveWaveData(LiveWave waveform, float[][] buf) {}
	public static record Ears (List<Channel> channels, Memory mem) {}

	private final Ears ears;
	private int maxFrames;

	public LiveWave(Dimension size, Ears listen) {
		super(size, new RMSRecording());

		ears = listen;
		attenuate(true);
		maxFrames = width * 2;
	}

	@Override
	public void generateImage() {
		if (recording.isEmpty())
			return;

		Graphics2D g2d = createGraphics();
	    g2d.setColor(bgColor);
	    g2d.fillRect(0, 0, width, height);

	    g2d.setColor(labelColor);
	    g2d.drawLine(0, centerY, width, centerY);

	    int pixel = 0;
	    int sample = 0;
	    int frames = recording.size();
	    if (frames < width) {
	    	samplesPerPixel = BUF_SIZE;
	    	pixel = width - frames; // whitespace on left
	    }
	    else if (frames > 2 * width) { // rolling right
	    	samplesPerPixel = 2 * BUF_SIZE;
	    	int startFrame = frames - 2 * width;
	    	sample = startFrame * BUF_SIZE;
	    }
	    else
	    	samplesPerPixel = (int) ((frames * BUF_SIZE) / (float)width);

	    if (workarea.length != samplesPerPixel)
	    	workarea = new float[samplesPerPixel];

	    float yFactor = yScale * AMPLITUDE * 6;
	    float iFactor = iScale * I_FACTOR * 6;
	    float iLength = iScale * AMPLITUDE * 6;
    	for (; pixel < width; pixel++) {
    		recording.getSamples(sample, workarea);
        	Buffer data = analyze(workarea);
        	int rmsIndex = (int) ((sample / (float)samplesPerPixel));
    		drawX(pixel, data, g2d, yFactor, iLength, I_SHIFT + (int) (recording.getRms(rmsIndex).left() * iFactor));
    		sample += samplesPerPixel;
    	}
	    if (printabels)
	    	printLabels(g2d);

	    g2d.dispose();
	}

	private void drawX(int pixel, Buffer data, Graphics2D g2d, float yFactor, float iLength, int rms) {
    	g2d.setColor(PEAKS);
        g2d.drawLine( pixel, centerY + (int)(data.peak().hi() * yFactor),
        			  pixel, centerY + (int)(data.peak().lo() * yFactor));

        g2d.setColor(Zwing.chaseTheRainbow(rms));
    	g2d.drawLine( pixel, centerY + (int)(data.amplitude().hi() * iLength),
        			  pixel, centerY + (int)(data.amplitude().lo() * iLength));
	}

	/** print to the graphics data of x (time and frame) and y scaling. */
	@Override
	protected void printLabels(Graphics2D g) {
	    int banner = 2 * MARGIN;
	    int baseline = (int) (height * 0.9f);
	    g.setColor(labelColor);
	    String chs = ears.channels().getFirst().toString() + (ears.channels().size() > 1 ? "+" : "");
	    g.drawString(chs, MARGIN, banner);
	    // bottom right: y scalings
	    int yλ = (int) Math.ceil(yScale * 100);
	    int iλ = (int) Math.ceil(iScale * 100);
	    String lambda = "yλ:" + yλ + " iλ:" + iλ;
	    int lambdaWidth = g.getFontMetrics().stringWidth(lambda);
	    g.drawString(lambda, width - MARGIN - lambdaWidth, baseline);

	}

	public void process() {
		float[][] buf = ears.mem.getFrame();
		ears.channels.forEach(ch->copy(ch, buf));
		MainFrame.update(new LiveWaveData(this, buf));
	}


	private void copy(Channel ch, float[][] work) {
		// consider Mains boost before speakers
		AudioTools.add(Mains.PREAMP, ch.getLeft(), work[LEFT]);
		AudioTools.add(Mains.PREAMP, ch.getRight(), work[RIGHT]);
	}

	public void update(float[][] stereo) {
		recording.add(stereo);
		while (recording.size() > maxFrames)
			recording.removeFirst();
		generateImage();
	}

}

//g.drawString(chs, MARGIN, baseline);
// top left/right time
//g.drawString(String.valueOf(AudioTools.sampleToSeconds(start)), INSET, banner);
//String finito = AudioTools.sampleToSeconds(end);
//// bottom left: zoom% of total Sec.
//String xλ = String.format("%s of %.2f", getZoom(), recording.seconds());
//g.drawString(xλ, INSET, baseline);
//// xFrame/totalFrame
////String frame = ((int) (start * BUF_INVERSE)) + "/" + recording.size();

