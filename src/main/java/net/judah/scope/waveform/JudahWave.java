package net.judah.scope.waveform;

import static net.judah.omni.WavConstants.LEFT;
import static net.judah.scope.waveform.WaveformKnobs.MAX_LIVE;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedList;

import javax.imageio.ImageIO;

import lombok.Getter;
import net.judah.omni.AudioTools;
import net.judah.omni.HiLo;
import net.judah.omni.Recording;
import net.judah.omni.WavConstants;
import net.judah.omni.Zwing;

/** Has a Recording, Displays an audio waveform
 *
 *    input audio wave form From?
 *
 *    process.
 *
 *    Process is an amalgam of selected channnels passed from Scope (view)
 *
 *
 *
 *
 *
 * */
@Getter
public class JudahWave extends BufferedImage {
    protected static final int TYPE = BufferedImage.TYPE_INT_ARGB;
	protected static final int TICK = 8;
    protected static final int I_FACTOR = 100;
//    public static final int MAX_LIVE = 2 ^ 8;

	private Color signalColor = new Color(57, 255, 20); // neon green
	private Color peakColor =  new Color(100, 149, 237); // cornflower blue
	private Color bgColor = Color.WHITE;

	private Source input = Source.LINE;
    protected final int width, height, center;
	private final Recording audio = new Recording();

    protected final LinkedList<HiLo> peaks = new LinkedList<>();
    protected final LinkedList<HiLo> amplitude = new LinkedList<>();
    protected final LinkedList<Integer> intensity = new LinkedList<>();

    protected int audioLength = MAX_LIVE; // frames * JACK_FRAME
    protected int start; // current audio window
    protected int range; // current audio window
    protected int samplesPerPixel;

	private int iShift = 40;  // shift color off blue
    private int yScale = 20; // scale amplitude 1 to 50
    private int iScale = 50; // scale intensity 1 to 100

    protected JudahWave(Dimension size) {
    	super(size.width, size.height, TYPE);
    	width = size.width;
		height = size.height;
		center = (int)(height * 0.5f);
		audioLength = MAX_LIVE;
		File audioFile = new File("/home/judah/Setlist/loops/Satoshi2.wav");

		//audio = new Recording(audioFile);
    }

	public JudahWave(Recording loop, Dimension size) {
		this(size);
		installRec(loop);
	}

	public JudahWave(File f, Dimension size) throws Exception {
		this(size);
		installRec(new Recording(f));
		input = Source.FILE;
	}


	public void process(float[][] frame) {
		while (audio.size() >= MAX_LIVE)
			audio.removeFirst();
		audio.add(frame);
		generateImage();
	}

	private void installRec(Recording rec) {
		input = Source.LOOP;
    	audioLength = audio.clone(rec);
    	fullRange();
	}

	private void installLine() {
		input = Source.LINE;
		audioLength = MAX_LIVE;
    	fullRange();
	}


	protected void generateImage() {
        Graphics g2d = initiateGraphics();
        g2d.setColor(peakColor);

//		if (input != Source.LINE) {
		peaks.clear(); intensity.clear(); amplitude.clear();

        for (int frame = 0; frame < width; frame++) // calculate fixed length
        	analyze(audio.range(start, samplesPerPixel));
//		}

		HiLo it;
		int frames = peaks.size();
		for (int frame = 0; frame < frames; frames++) {
	    	it = peaks.get(frame);
	        g2d.drawLine(
	        		frame, (int)(center + it.hi() * yScale),
	        		frame, (int)(center + it.lo() * yScale));

	        it = amplitude.get(frame);
	        g2d.setColor(Zwing.chaseTheRainbow(intensity.get(frame) + iShift));
	    	g2d.drawLine(
	        		frame, (int)(center + it.hi() * yScale),
	        		frame, (int)(center + it.lo() * yScale));
		}

        // paint some time ticks
        g2d.setColor(Color.BLACK);
        for (int x : new int[] {0, (int) (width * 0.25f), width / 2, (int) (width * 0.75f), width})
            g2d.drawLine(x, height - TICK, x, height);
        // label time window
//        // width of caret string, right justify if it falls off right side of frame
//        int nowWidth = g2d.getFontMetrics().stringWidth(now);
//        if (caret + nowWidth + MARGIN < width)
//        	g2d.drawString(now, caret + MARGIN, TIMELINE);
//        else //
//        	g2d.drawString(now, caret - POP_LEFT, TIMELINE);

        g2d.dispose();
	}

	//@see AudioTools.peaks + AudioTools.avg
	// TODO isMono
	void analyze(float[][] in) {

	    float sumPositive = 0;
	    float sumNegative = 0;
	    int countPositive = 0;
	    int countNegative = 0;
	    float min = Float.MAX_VALUE;
	    float max = Float.MIN_VALUE;

	    for (float val : in[LEFT]) {
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

	    // FIFO
		if (input == Source.LINE && amplitude.size() == width) {
			amplitude.removeFirst();
			peaks.removeFirst();
			intensity.removeFirst();
		}
    	amplitude.addLast(new HiLo(avgPositive, avgNegative));
    	peaks.addLast(new HiLo(max, min));
        intensity.addLast((int) (AudioTools.intensity(in[LEFT]) * iScale * I_FACTOR));

	}

	/** save Waveform image to disk as png */
	public void save(File file) {
      try {
          ImageIO.write(this, "png", file);
      } catch (java.io.IOException e) {
          e.printStackTrace();
      }
	}

	public void saveAs() {
		// TODO Auto-generated method stub
	}


	protected void fullRange() {
		setRange(0, audioLength - 1);
	}

	protected void setRange(int begin, int stop) {
	    int range = stop - begin;
	    int max = audioLength - 1;
	    if (range > max)
	    	range = max;
		if (begin < 0) {
	        begin = 0;
	        stop = begin + range; // shift
		}
	    if (stop > max) {
	        stop = max;
	        begin = max - range;  // shift
	    }
		start = begin;
		samplesPerPixel = (int) ((stop - start) / (float)width);
		generateImage();
		//System.out.println(begin + " to " + stop + "  After: " + start + " to " + end + " range: " + range + " spp: " + samplesPerPixel);
	}

	protected Graphics2D initiateGraphics() {
		Graphics2D g2d = createGraphics();
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, width, height);
        return g2d;
	}

	public long caretToMillis(int caret) {
	    // Calculate the visible portion length and zoom factor
		int end = start + range;
	    int visibleLength = end - start;
	    float zoomFactor = (float) audioLength / visibleLength;

	    // Calculate the sample position considering the zoom factor
	    long samplePosition = (long) (caret * zoomFactor);

	    // Convert the sample position to milliseconds
	    return (long) ((samplePosition / WavConstants.S_RATE) * 1000f);
	}

//	private float[][] pixelSamples(int start) {
//		float[] left = new float[samplesPerPixel];
//		float[] right = new float[samplesPerPixel];
//		float [][] result = new float[STEREO][];
//		result[LEFT] = left;
//		result[RIGHT] = right;
////		System.arraycopy(audio[LEFT], start, left, 0, samplesPerPixel);
////		System.arraycopy(audio[RIGHT], start, right, 0, samplesPerPixel);
//		audio.arrayCopy(start, samplesPerPixel, result);
//		float[][] pixel = audio.range(start, samplesPerPixel);
//		return result;
//	}

	public void setYScale(int amplitude) { // scale amplitude 1 to 50
		if (amplitude < 1 || amplitude > 50)
			return;
		yScale = amplitude;
		generateImage();
	}

	public void intensity(int i) { // scale intensity 1 to 100
		if (i < 1 || i > 100)
			return;
		iScale = i;
		generateImage();
	}
    public void setSignalColor(Color c) {
    	signalColor = c;
    	generateImage();
    }
    public void setPeakColor(Color c) {
    	peakColor = c;
    	generateImage();
    };
    public void setBgColor(Color c) {
    	bgColor = c;
    	generateImage();
    }

	public void intensity(boolean up) {
		if (up) {
			intensity(iScale + 10);
			setYScale(yScale + 5);
		}
		else {
			intensity(iScale - 10);
			setYScale(yScale - 5);
		}
	}

	public void scroll(float percent) {
		if (percent < 0 || percent > 1) return;
		int begin = (int) (audioLength * percent);
		if (begin + range > audioLength)
			begin = range - audioLength;
		setRange(begin, begin + range);
	}

	public void scroll(boolean up) { // shift-wheel  ratio: start to end
		int delta = (up ? -1 : 1) * (int)(.25f * width * samplesPerPixel);
	    // Calculate new start and end positions
	    double newStart = Math.max(0, start + delta);
	    double newEnd = Math.min(audioLength, start + range + delta);

	    // Ensure the new range maintains the current width
	    if (newEnd - newStart < range)
	        newStart = newEnd - range;

	    // Set the new range
	    setRange((int) newStart, (int) newEnd);
	}

	public void zoom(float percent) {
		if (percent < 0 || percent > 1) return;
		int girth = (int) (audioLength * percent);
		int end = start + range;
	    int center = (start + end) / 2;  // or center around caret?
		int begin = center - girth / 2;
		if (begin < 0)
			begin = 0;
		setRange(begin, begin + girth);
	}

	public void zoom(boolean up) { // ctrl-wheel  ratio: min=all  max=1 atom:?
	    double zoomFactor = up ?  0.8 : 1.25;
	    int end = start + range;
	    int currentWidth = end - start;
	    int newWidth = (int) (currentWidth * zoomFactor);
	    int center = (start + end) / 2;

	    int newStart = center - newWidth / 2;
	    int newEnd = center + newWidth / 2;
	    setRange(newStart, newEnd);
	}



}
