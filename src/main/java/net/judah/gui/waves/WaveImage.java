package net.judah.gui.waves;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import lombok.Getter;
import net.judah.omni.HiLo;
import net.judah.omni.Zwing;
import net.judah.util.Constants;
import net.judah.util.Folders;

public abstract class WaveImage extends BufferedImage {
	public static record Buffer(HiLo peak, HiLo amplitude) {	}

	protected static final int BUF_SIZE = Constants.bufSize();
	protected static final int S_RATE = Constants.sampleRate();
	protected static final int I_FACTOR = 200; // boost RMS into color range
	protected static final int AMPLITUDE = 400; // scale peaks into pixels
    protected static final int I_SHIFT = 36; // shift intensity off blue
	public static final int INSET = 4;
	public static final int MARGIN = INSET * 2;

	@Getter protected final RMSRecording recording;
    @Getter protected int samplesPerPixel, zoomCenter;
    protected int start, end, range; // user adjust audio window
    protected float yScale = 0.5f; // user adjust amplitude 0 to 1
    protected float iScale = 0.5f; // user adjust intensity 0 to 1
	protected int audioLength;
	protected final int width, height, centerY; // pixels
    protected float[] workarea;

    @Getter protected final Color PEAKS = Zwing.chaseTheRainbow(33);
    @Getter protected boolean printabels = true;
    @Getter protected Color labelColor = Color.RED;
    @Getter protected Color bgColor = Color.WHITE;
    @Getter protected File toDisk = new File("waveform.png");

	public WaveImage(Dimension size, RMSRecording rmsRec) { // fixed length
		super(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		this.recording = rmsRec;
		audioLength = recording.size() * BUF_SIZE;
		zoomCenter = audioLength / 2;
		width = size.width;
		height = size.height;
		centerY = (int)(height * 0.5f);
		fullRange();
	}

	protected abstract void generateImage();
	protected abstract void printLabels(Graphics2D g);

	public boolean inWindow(long millis) {
		return false;
	}

	//see AudioTools.peaks + avg
	final Buffer analyze(float[] in) {
	    float sumPositive = 0;
	    float sumNegative = 0;
	    int countPositive = 0;
	    int countNegative = 0;
	    float min = Float.MAX_VALUE;
	    float max = Float.MIN_VALUE;

	    for (float val : in) {
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
	    return new Buffer(new HiLo(max, min), new HiLo(avgPositive, avgNegative));
	}

	/** shift yScale and iScale at the same time (ctrl-shift mouseWheel) */
	public void attenuate(boolean up) {
		float i = iScale * 0.1f;
		float y = yScale * 0.1f;

		if (up) {
			setIntensity(iScale + i);
			setYScale(yScale + y);
		}
		else {
			setIntensity(iScale - i);
			setYScale(yScale - y);
		}
	}

	public void setIntensity(float val) {
		if (val < 0 || val > 1)
			return;
		iScale = val;
		if (this instanceof FileWave)
			generateImage();
	}
	public void setYScale(float val) {
		if (val < 0 || val > 1)
			return;
		yScale = val;
		if (this instanceof FileWave)
			generateImage();
	}

	public void scroll(boolean up) { // mouse-wheel  ratio: start to end
		int delta = (up ? -1 : 1) * (int)(.25f * width * samplesPerPixel);
	    // Calculate new start and end positions
	    double newStart = Math.max(0, start + delta);
	    double newEnd = Math.min(audioLength, end + delta);

	    // Ensure the new range maintains the current width
	    if (newEnd - newStart < range)
	        newStart = newEnd - range;

	    // Set the new range
	    if (newEnd - newStart < BUF_SIZE)
	    	return;
	    setRange((int) newStart, (int) newEnd);
	    rezoom();
	}

	private void rezoom() {
		zoomCenter = (start + end) / 2;
	}

	/** absolute sample index */
	protected void setRange(int begin, int stop) {
		range = stop - begin;
	    int max = audioLength - 1;
	    if (range > max)
	    	range = max;
	    if (range < BUF_SIZE)
	    	range = BUF_SIZE;
		if (begin < 0) {
	        begin = 0;
	        stop = begin + range; // shift
		}
	    if (stop > max) {
	        stop = max;
	        begin = max - range;  // shift
	    }
	    if (begin == start && stop == end)
	    	return;
		start = begin;
		end = stop;

		samplesPerPixel = (int) ((end - start) / (float)width);

		// if (samplesPerPixel == 0) throw new InvalidParameterException();

		workarea = new float[samplesPerPixel];
		generateImage();
	}
	protected void fullRange() {
		setRange(0, audioLength - 1);
	}

	// TODO, if fully zoomed out, move zoomCenter
	public void setX(float amount) {
		// if 0 = 100% left of zoom center, 100 = 100% right of zoom Center
		// 100% means 1 viewport, 1 whole width in pixels left or right
		int width = (int) (getRange() * audioLength);
		int half = (int) (width * 0.5f);
		float justified = amount - 0.5f;
		float factor = justified * 2; // -1 to +1
		int x = (zoomCenter - half) + (int) (width * factor);
		setRange(x, x + width);
	}

	public void setXScale(float amount) {
		int newSize = (int) (audioLength * amount);
	    if (newSize < BUF_SIZE)
	    	return; // too small
		int half = (int) (newSize * 0.5f);

	    rezoom();
	    setRange(zoomCenter - half, zoomCenter + half - 1);
	}

	public void zoom(boolean up) { // ctrl-wheel  ratio: min=all  max=1 atom:?
	    setXScale(getRange() * (up ? 0.8f : 1.25f));
	}

	public void save() {
		save(toDisk);
	}

	public void saveAs() {
		File file = Folders.choose();
		if (file == null)
			return;
		save(file);
	}

	/** save Waveform image to disk as png */
	public void save(File file) {
      try {
          ImageIO.write(this, "png", file);
          toDisk = file;
      } catch (java.io.IOException e) {
          e.printStackTrace();
      }
	}

	public float getRange() {
		return (end - start) / (float)audioLength;
	}

	public String getZoom() {
        int zoomPercent = (int) (100 * getRange());
        return zoomPercent + "%";
	}

	public void setLabelColor(Color c) {
		labelColor = c;
		generateImage();
	}

    public void setBgColor(Color c) {
    	bgColor = c;
    	generateImage();
    }
	public void toggleLables() {
		printabels = !printabels;
		generateImage();
	}

	public int pixelOf(int sampleIdx) {
		if (sampleIdx < start)
			return -1;
		if (sampleIdx > end)
			return -1;
		if (samplesPerPixel == 0)
			return 0;
		return (sampleIdx - start) / samplesPerPixel;
	}

}

//public long caretToMillis(int caret) {
//    // Calculate the visible portion length and zoom factor
//    int visibleLength = end - start;
//    float zoomFactor = (float) audioLength / visibleLength;
//    // Calculate the sample position considering the zoom factor
//    long samplePosition = (long) (caret * zoomFactor);
//    // Convert the sample position to milliseconds
//    return (long) ((samplePosition / S_RATE) * 1000f);
