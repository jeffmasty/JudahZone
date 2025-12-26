package net.judah.gui.scope;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class TimeDomain extends JPanel implements Gui.Mouser {

	private enum DragMode { OFF, DRAG, IGNORE }

	static final int FFT_SIZE = Constants.fftSize();

	static final int HEIGHT_VIEWPORT = 192;
	static final int HEIGHT_RMS = HEIGHT_VIEWPORT / 3;
	static final int HEIGHT_SPECTRUM = HEIGHT_RMS * 2;
	static final int HEIGHT_5050 = HEIGHT_VIEWPORT - (HEIGHT_RMS/2);
	static Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
	                                  0, new float[]{9}, 0);

	//static final int HEIGHT_RMS = 192;
	/** Y position (pixels) for drawing time labels, just below RMS top. */
	static final int HEIGHT_LBL = 150;
	static final Color HEAD = Pastels.PURPLE;
	static final Color GUAGE = Pastels.FADED;

	private final JudahScope scope;
	private final Transform[] db;
	private Spectrogram spectro;
	private RMSMeter rms;

	/** Per-instance controls (RMS gain, zoom). */
	private final JPanel controls = new JPanel();
	private JSlider rmsSlider;
	private JSlider zoomSlider;
	private final boolean zoomable;

	/** Caret position in db indices (not pixels). */
	private int positionIndex;

	private int w;
	/** Pixels per db index in the current viewport (can be fractional). */
	private float unit = 1f;

	private DragMode inDrag = DragMode.OFF;

	/** Index viewport over db[], WaveImage-style. */
	private int startIndex = 0;
	private int endIndex;
	private int viewportSize;
	private int zoomCenterIndex;

	/** Minimum number of frames visible at maximum zoom. */
	private static final int MIN_VISIBLE_FRAMES = 16;
	boolean live;

	// Live
	TimeDomain(TimeDomain source, int width) {
		this(source.scope, width, false, source.db);
		live = true;
	}

	// Paused
	TimeDomain(JudahScope view, int width) {
		this(view, width, true, new Transform[width / 2]);
	}

	// File (or zoomable paused)
	TimeDomain(JudahScope view, int width, boolean zoomable, Transform[] db) {
		this.scope = view;
		this.db = db;
		this.zoomable = zoomable;
		endIndex = db.length - 1;
		if (zoomable) {
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
		}
		initControls();
		resize(width);
		fullRange(); // initialize viewport (all 3 cases come here)
	}

	public JPanel getControls() {
		return controls;
	}

	private void initControls() {
		controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));

		// RMS Gain slider: 0..100 -> 0..1 gain
		rmsSlider = new JSlider(0, 100, 50);
		rmsSlider.addChangeListener(e -> setYScaleInternal(rmsSlider.getValue() * 0.01f));

		controls.add(Box.createHorizontalStrut(8));
		controls.add(new JLabel(" RMS "));
		controls.add(Gui.resize(rmsSlider, Size.SMALLER));

		if (zoomable) {
			zoomSlider = new JSlider(1, 100, 100); // 1 = fully out, 100 = max in
			zoomSlider.setValue(1);
			zoomSlider.addChangeListener(e -> setZoomScale(zoomSlider.getValue() * 0.01f));
			controls.add(Box.createHorizontalStrut(8));
			controls.add(new JLabel(" Zoom "));
			controls.add(Gui.resize(zoomSlider, Size.SMALLER));
		}
		controls.add(Box.createHorizontalStrut(8));
	}

	private int visibleLength() {
		return endIndex - startIndex + 1;
	}

	void fullRange() {
		setRange(0, db.length - 1);
	}

	/** Set absolute index range (inclusive) for viewport, clamp to db size. */
	private void setRange(int begin, int stop) {
		if (db.length == 0) {
			startIndex = endIndex = 0;
			viewportSize = 0;
			zoomCenterIndex = 0;
			return;
		}

		int max = db.length - 1;
		if (begin < 0)
			begin = 0;
		if (stop > max)
			stop = max;

		if (stop < begin) {
			stop = begin;
		}

		startIndex = begin;
		endIndex = stop;
		viewportSize = visibleLength();
		zoomCenterIndex = (startIndex + endIndex) / 2;

		updateUnitFromViewport();
		regenerateChildren();
		repaint();
	}

	private void updateUnitFromViewport() {
		if (w <= 0 || viewportSize <= 0) {
			unit = 1f;
		} else {
			unit = w / (float) viewportSize;
		}
	}

	private void setPositionByPixel(int xPixel) {
		if (unit <= 0)
			unit = 1f;
		int relativeIndex = Math.round(xPixel / unit);
		int idx = startIndex + relativeIndex;
		if (idx < startIndex)
			idx = startIndex;
		if (idx > endIndex)
			idx = endIndex;
		positionIndex = idx;
		repaint();
	}

	private int caretX() {
		return Math.round((positionIndex - startIndex) * unit);
	}

	public void analyze(Transform data) {
		if (positionIndex < 0 || positionIndex >= db.length)
			return;
		db[positionIndex] = data;

		int xPixel = caretX();
		int nextX = Math.round((positionIndex + 1 - startIndex) * unit);
		int cellWidth = Math.max(1, nextX - xPixel);

		rms.analyze(xPixel, data, cellWidth);
		spectro.analyze(xPixel, data, cellWidth);

		increment();
	}

	void generate() {
		regenerateChildren();
		repaint();
	}

	private void regenerateChildren() {
		if (rms == null || spectro == null)
			return;
		updateUnitFromViewport();
		rms.generateImage(unit, startIndex, endIndex);
		spectro.generateImage(unit, startIndex, endIndex);
	}

	private void increment() {
		positionIndex++;
		if (positionIndex > endIndex)
			positionIndex = startIndex;
		invalidate();
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		spectro.drawBorder();
		rms.drawBorder();

		g.drawImage(spectro, 0, 0, null);
		g.drawImage(rms, 0, spectro.h, null);
		g.setColor(GUAGE);

		Graphics2D g2 = (Graphics2D)g;

		Stroke reset = g2.getStroke();
		((Graphics2D)g).setStroke(dashed);
		g.drawLine(0, HEIGHT_5050, getWidth(), HEIGHT_5050);
		g2.setStroke(reset);

		// caret
		g.setColor(HEAD);
		int x = caretX();
		g.drawLine(x, 0, x, HEIGHT_VIEWPORT);

		// time labels (only on zoomable domains)
		if (zoomable) {
			drawFrameLabels(g);
		}
	}

	/** Draw FontMetrics size below HEIGHT_VIEWPORT
	 * left ->   write the left index into fft data array of where you think the left of the viewport is
	 * 			at any given repaint() then write a backslash then an integer percent of zoom with a percent character
	 * right ->  write right index into fft data array of where you think the right edge of the viewport is
	 * 		then a backslash then the total number of frames
	 * head ->   write directly under the HEAD position which frame we are on.
	 * */
	private void drawFrameLabels(Graphics g) {
		java.awt.FontMetrics fm = g.getFontMetrics();
		// clear previous labels in one go (no decorative fills, just clear the label strip)
		int clearY = HEIGHT_VIEWPORT;
		int clearH = Size.STD_HEIGHT;
		g.clearRect(0, clearY, getWidth(), clearH);

		// left label: "startIndex\zoom%"
		int maxSize = db.length;
		int vis = Math.max(1, visibleLength());
		int percentVisible = Math.round((vis * 100f) / Math.max(1, maxSize));
		String leftLabel = startIndex + "\\" + percentVisible + "%";

		// right label: "endIndex\totalFrames"
		String rightLabel = endIndex + "\\" + db.length;

		// head label: frame at caret position
		String headLabel = Integer.toString(positionIndex);

		int pad = 4;
		int baselineY = clearY + fm.getAscent() + 2;

		g.setColor(Color.BLACK);
		// left (left-aligned)
		int leftX = pad;
		g.drawString(leftLabel, leftX, baselineY);

		// right (right-aligned)
		int rightX = getWidth() - fm.stringWidth(rightLabel) - pad;
		if (rightX < leftX + fm.stringWidth(leftLabel) + 8) {
		    // avoid drawing completely off-edge if very small; clamp
		    rightX = Math.max(rightX, leftX + fm.stringWidth(leftLabel) + 8);
		}
		g.drawString(rightLabel, rightX, baselineY);

		// head (centered under caret)
		int cx = caretX();
		int headWidth = fm.stringWidth(headLabel);
		int headX = cx - (headWidth / 2);
		// clamp to panel edges so it stays visible
		headX = Math.max(pad, Math.min(headX, getWidth() - headWidth - pad));
		g.drawString(headLabel, headX, baselineY);
	}

	void resize(int w) {
		if (rms != null)
			try {
				rms.close();
				spectro.close();
			} catch (Exception e) {
				RTLogger.warn(this, e);
			}
		this.w = w;
		rms = new RMSMeter(new Dimension(w, HEIGHT_RMS), db);
		spectro = new Spectrogram(new Dimension(w, HEIGHT_SPECTRUM), db);

		Dimension sz = new Dimension(w, HEIGHT_VIEWPORT + Size.STD_HEIGHT);
		Gui.resize(this, sz).setMaximumSize(sz);
		updateUnitFromViewport();
		regenerateChildren();
		revalidate();
		repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		setPositionByPixel(e.getPoint().x);
		scope.click(db[positionIndex]);
		inDrag = DragMode.DRAG;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		inDrag = DragMode.OFF;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		int oldPos = positionIndex;
		setPositionByPixel(e.getPoint().x);
		if (positionIndex == oldPos)
			return;
		if (inDrag != DragMode.DRAG)
			return;
		scope.click(db[positionIndex]);
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (inDrag == DragMode.DRAG)
			inDrag = DragMode.IGNORE;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (inDrag == DragMode.IGNORE)
			inDrag = DragMode.DRAG;
	}

	private void setYScaleInternal(float val) {
		rms.both((int) (val * 100f), 0);
		if (live)
			return;
		regenerateChildren();
		repaint();
	}

	void setYScale(int val) {
		float f = val * 0.01f;
		setYScaleInternal(f);
		rmsSlider.setValue(val);
	}

	/** Slider: amount in [0,1]: 0=full range, 1=max zoom (=16 frames). */
	private void setZoomScale(float amount) {
		if (!zoomable || db.length == 0)
			return;

		if (amount <= 0f) {
			fullRange();
			return;
		}

		int maxSize = db.length;
		int minSize = Math.min(MIN_VISIBLE_FRAMES, maxSize);

		// amount 0..1 maps to size maxSize..minSize
		int newSize = (int) (maxSize - (maxSize - minSize) * amount);
		if (newSize < minSize)
			newSize = minSize;
		if (newSize > maxSize)
			newSize = maxSize;

		zoomCenterIndex = positionIndex;
		int half = newSize / 2;
		int newStart = zoomCenterIndex - half;
		int newEnd = newStart + newSize - 1;

		if (newStart < 0) {
			newStart = 0;
			newEnd = newSize - 1;
		}
		if (newEnd >= db.length) {
			newEnd = db.length - 1;
			newStart = newEnd - newSize + 1;
			if (newStart < 0)
				newStart = 0;
		}
		setRange(newStart, newEnd);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		boolean up = e.getWheelRotation() < 0;

		boolean isCtrlShiftPressed = (e.getModifiersEx()
				& (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)) == (InputEvent.CTRL_DOWN_MASK
						| InputEvent.SHIFT_DOWN_MASK);
		boolean isCtrlPressed = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK
				&& (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0;
		boolean isShiftPressed = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK
				&& (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0;

		if (isCtrlShiftPressed) {
			rms.attenuate(up, 0);
			regenerateChildren();
			repaint();
		} else if (isCtrlPressed) {
			// zoom in/out over db[] using the same MIN_VISIBLE_FRAMES clamp
			zoom(zoomable && up);
			if (zoomable && zoomSlider != null) {
				int maxSize = db.length;
				int minSize = Math.min(MIN_VISIBLE_FRAMES, maxSize);
				float amount = (maxSize == minSize)
						? 1f
						: (1f - ((visibleLength() - minSize) / (float) (maxSize - minSize)));
				if (amount < 0f) amount = 0f;
				if (amount > 1f) amount = 1f;
				zoomSlider.setValue((int) (amount * 100f));
			}
		} else if (isShiftPressed) {
			int incrementFactor = Math.max(1, visibleLength() / 20);
			int newIndex = positionIndex + (up ? -incrementFactor : incrementFactor);
			if (newIndex < startIndex)
				newIndex = startIndex;
			if (newIndex > endIndex)
				newIndex = endIndex;
			positionIndex = newIndex;
			scope.click(db[positionIndex]);
			repaint();
		} else {
			scroll(up);
		}
	}

	private void scroll(boolean left) {
		if (db.length == 0 || viewportSize <= 0)
			return;
		int delta = (left ? -1 : 1) * (int) (0.25f * viewportSize);
		int newStart = startIndex + delta;
		int newEnd = endIndex + delta;

		if (newStart < 0) {
			newStart = 0;
			newEnd = newStart + viewportSize - 1;
		}
		if (newEnd >= db.length) {
			newEnd = db.length - 1;
			newStart = newEnd - viewportSize + 1;
			if (newStart < 0)
				newStart = 0;
		}
		setRange(newStart, newEnd);
	}

	/** Zoom helper, clamped by MIN_VISIBLE_FRAMES for zoomable domains. */
	private void zoom(boolean zoomIn) {
		if (!zoomable || db.length == 0)
			return;

		int length = visibleLength();
		if (length <= 0)
			length = 1;

		float factor = zoomIn ? 0.8f : 1.25f;
		int newSize = Math.round(length * factor);

		int maxSize = db.length;
		int minSize = Math.min(MIN_VISIBLE_FRAMES, maxSize);

		if (newSize < minSize)
			newSize = minSize;
		if (newSize > maxSize)
			newSize = maxSize;

		zoomCenterIndex = positionIndex;
		int half = newSize / 2;
		int newStart = zoomCenterIndex - half;
		int newEnd = newStart + newSize - 1;

		if (newStart < 0) {
			newStart = 0;
			newEnd = newSize - 1;
		}
		if (newEnd >= db.length) {
			newEnd = db.length - 1;
			newStart = newEnd - newSize + 1;
			if (newStart < 0)
				newStart = 0;
		}
		setRange(newStart, newEnd);
	}
}