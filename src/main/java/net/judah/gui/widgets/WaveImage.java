package net.judah.gui.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import judahzone.gui.Pastels;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.RTLogger;

public abstract class WaveImage extends BufferedImage implements Closeable {

	protected static final int JACK_BUFFER = Constants.bufSize();
	protected static final int S_RATE = Constants.sampleRate();
	protected final Color BACKGROUND = Color.WHITE;
	protected final Color HEAD = Pastels.PURPLE;

    protected int samplesPerPixel;
    protected int zoomCenter;
    protected int start, end, range; // user adjust audio window
	protected int audioLength;
	protected final int width, height, centerY; // pixels

    protected boolean printabels;
	protected final Graphics2D g2d;

	public WaveImage(Dimension size) {
		super(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		g2d = createGraphics();
		width = size.width;
		height = size.height;
		g2d.setBackground(BACKGROUND);
		g2d.clearRect(0, 0, width, height);
		centerY = (int)(height * 0.5f);
	}

	public int getSamplesPerPixel() {
		return samplesPerPixel;
	}
	public int getZoomCenter() {
		return zoomCenter;
	}
	public boolean isPrintLables() {
		return printabels;
	}

	public void length(int frames) {
		audioLength = frames * JACK_BUFFER;
		zoomCenter = audioLength / 2;
		fullRange();
		RTLogger.log(this, "frames: " + frames);
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
	    if (newEnd - newStart < JACK_BUFFER)
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
	    if (range < JACK_BUFFER)
	    	range = JACK_BUFFER;
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

	}
	protected void fullRange() {
	    zoomCenter = audioLength / 2;
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
	    if (newSize < JACK_BUFFER)
	    	return; // too small
		int half = (int) (newSize * 0.5f);

	    rezoom();
	    setRange(zoomCenter - half, zoomCenter + half - 1);
	}

	public void zoom(boolean up) { // ctrl-wheel  ratio: min=all  max=1 atom:?
	    setXScale(getRange() * (up ? 0.8f : 1.25f));
	}

	public void save() {
		save(new File("waveform.png"));
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
          // toDisk = file;
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

	public int pixelOf(int sampleIdx) {
		if (sampleIdx < start)
			return -1;
		if (sampleIdx > end)
			return -1;
		if (samplesPerPixel == 0)
			return 0;
		return (sampleIdx - start) / samplesPerPixel;
	}

	@Override public final void close() throws IOException {
		g2d.dispose();
	}

}


//private void drawZoomCenter(Graphics2D g) {
//	if (!waveform.isPrintabels())
//		return;
//	int zoomCenter = waveform.pixelOf(waveform.getZoomCenter());
//	if (zoomCenter < 0)
//		return;
//	g.setStroke(Zwing.DASHED_LINE);
//	g.setColor(Pastels.MY_GRAY);
//	g.drawLine(zoomCenter, INSET, zoomCenter, height - INSET);
//}

//private void drawCaret(Graphics2D g) {
//if (!waveform.inWindow(current))
//	return;
//// Draw caret
//g.setColor(caretColor);
//g.fillOval(caret - 2, getHeight() - MARGIN, MARGIN, MARGIN);
//g.setStroke(Zwing.DASHED_LINE);
//g.drawLine(caret + 2, INSET, caret + 2, height);
//// right justify now string if off right edge
//int nowWidth = g.getFontMetrics().stringWidth(now);
//if (caret + nowWidth + MARGIN < width)
//	g.drawString(now, caret + MARGIN, TIMELINE);
//else //
//	g.drawString(now, caret - POP_LEFT, TIMELINE);
//}

//public long caretToMillis(int caret) {
//    // Calculate the visible portion length and zoom factor
//    int visibleLength = end - start;
//    float zoomFactor = (float) audioLength / visibleLength;
//    // Calculate the sample position considering the zoom factor
//    long samplePosition = (long) (caret * zoomFactor);
//    // Convert the sample position to milliseconds
//    return (long) ((samplePosition / S_RATE) * 1000f);

