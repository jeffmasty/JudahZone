package net.judah.gui.scope;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;

import net.judah.gui.Pastels;

/** Display something (spectrum, RMS) in the Time Domain */
public abstract class TimeWidget extends BufferedImage implements Closeable {

	protected static final Color BACKGROUND = Color.WHITE;
	protected static final Color HEAD = Pastels.PURPLE;

	protected int w, h;
	protected final Graphics2D g2d;
	protected final Transform[] db;

	public TimeWidget(Dimension size, Transform[] db) { // fixed length
		super(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		this.db = db;
		w = size.width;
		h = size.height;
		g2d = createGraphics();
		g2d.setBackground(BACKGROUND);
		g2d.clearRect(0, 0, w, h);
	}

    abstract void generateImage(float unit);
    abstract void analyze(int x, Transform t, int cell);

	public void clearRect(int x, int width) {
		g2d.clearRect(x, 0, width, h);
	}

    protected void drawBorder() {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawLine(0, 0, w, 0);
        g2d.drawLine(w - 1, 0, w - 1, h);
        g2d.drawLine(0, h - 1, w, h - 1);
        g2d.drawLine(0, 0, 0, h);
    }

	@Override public final void close() throws IOException {
		g2d.dispose();
	}

}
