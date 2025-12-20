package net.judah.gui.scope;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JPanel;

import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

// TODO TD generates from db[x] to db[y] at (unit) pixels per index
// live is always 0 to length at (3) units per pixel
// otherwise, unit = (pixels / y - x)
public class TimeDomain extends JPanel implements Gui.Mouser {

	private enum DragMode { OFF, DRAG, IGNORE }

	static final int FFT_SIZE = Constants.fftSize();
	static final int HEIGHT_RMS = 192;
	static final int HEIGHT_LBL = 200;
	static final Color HEAD = Pastels.PURPLE;

	private final JudahScope scope;
	private final Transform[] db;
	private Spectrogram spectro;
	private RMSMeter rms;
	private int position;
	private int unit = 1;
	private DragMode inDrag = DragMode.OFF;

	// Live
	TimeDomain(TimeDomain source, int width) {
		this(source.scope, width, false, source.db);
	}

	// Paused
	TimeDomain(JudahScope view, int width) {
		this(view, width, true, new Transform[width]);
	}

	// File
	TimeDomain(JudahScope view, int width, boolean zoomable, Transform[] db) {
		this.scope = view;
		this.db = db;
		if (zoomable) {
//			if (db.length > width)
//				unit = (int) (db.length / (float)width);
//			else if (db.length < width)
//				unit = (int) (width / (float)db.length);
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
		}
		resize(width);
	}

	private void setPosition(int x) {
		if (x < 0) x = 0;
		if (x >= db.length) x = db.length - 1;
		position = x;
		repaint();
	}


	public void analyze(Transform data) {
		db[position] = data;
		rms.analyze(position, data, unit);
		spectro.analyze(position, data, unit);
		increment();
	}

	void generate() {
		//	spp();
		rms.generateImage(unit);
		spectro.generateImage(unit);
		repaint();
	}

	private void increment() {
    	position += unit;
    	position = position %  getWidth();
    	invalidate();
		repaint();
	}

	@Override public void paint(Graphics g) {
		spectro.drawBorder();
		rms.drawBorder();

		g.drawImage(spectro, 0, 0, null);
		g.drawImage(rms, 0, spectro.h, null);

		g.setColor(HEAD); // draw caret
		g.drawLine(position, 0, position, HEIGHT_RMS);
	}

	void resize(int w) {
		if (rms != null) try {
			rms.close();
			spectro.close();
		} catch (Exception e) { RTLogger.warn(this, e); }

		rms = new RMSMeter(new Dimension(w, (int) (HEIGHT_RMS * 1/3f)), db);
		spectro = new Spectrogram(new Dimension(w, (int) (HEIGHT_RMS * 2/3f)), db);

		Dimension sz = new Dimension(w, HEIGHT_RMS);
		Gui.resize(this, sz).setMaximumSize(sz);
		revalidate();
		repaint();
	}

	@Override public void mousePressed(MouseEvent e) {
		setPosition(e.getPoint().x);
		scope.click(db[position]);
		inDrag = DragMode.DRAG;
	}
	@Override public void mouseReleased(MouseEvent e) {
		inDrag = DragMode.OFF;
	}
	@Override public void mouseDragged(MouseEvent e) {
		if (position == e.getPoint().x)
			return;
		if (inDrag != DragMode.DRAG)
			return;
		setPosition(e.getPoint().x);
		scope.click(db[position]);
	}
	@Override public void mouseExited(MouseEvent e) {
		if (inDrag == DragMode.DRAG)
			inDrag = DragMode.IGNORE;
	}

	@Override public void mouseEntered(MouseEvent e) {
		if (inDrag == DragMode.IGNORE)
			inDrag = DragMode.DRAG;
	}

	void setYScale(int val) {
		rms.both(val, unit);
		repaint();
	}

	@Override public void mouseWheelMoved(MouseWheelEvent e) {
		boolean up = e.getWheelRotation() < 0;

	    boolean isCtrlShiftPressed = (e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))
	    		==  (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	    boolean isCtrlPressed = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK)  == InputEvent.CTRL_DOWN_MASK
	    		&& (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0;
	    boolean isShiftPressed = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK
	            && (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0;

	    if (isCtrlShiftPressed) {
	    	rms.attenuate(up, unit);
	    	repaint();
	    }
	    else if (isCtrlPressed) {
//	    	zoom(up);
//	    	repaint();
	    } else if (isShiftPressed) { // shift caret left or right
			int incrementFactor = getWidth() / 20; /*cycles*/;
			setPosition(position + (up ? -incrementFactor : incrementFactor));
			scope.click(db[position]);
	    }
	    else { //no modifiers = scroll left or right
//	    	scroll(up); // scroll left/right
//	    	repaint();
	    }
	}
}

//TODO RMS Slider gain
//private final JSlider zoomSlider = new JSlider(1, 100, 50); // go bigger?
//private final Box zoomDuo = Gui.box(new JLabel(" Zoom "), Gui.resize(zoomSlider, Size.SMALLER));

//@Getter private int start, end;
//private int samplesPerPixel = FFT_SIZE; // (minimum?)
//private int audioLength;
//private int range;
//private float zoom = 1f; // for offline processing
//private int zoomCenter;

///** set samplesPerPixel based on state and available data */
//private void spp() {
//    // how much a pixel = an FFT_SIZE frame  LIVE:  1=1FFT  2=1/2FFT  3(default?)= 1/3FFT 4=JACK_BUFFER
//	// < 1 zoom way out? max = 10 minutes?
//	float width = w;
//	samplesPerPixel = switch (status) {
//		case LIVE_ROLLING -> FFT_SIZE * STEP;
//		case LIVE_STOPPED -> FFT_SIZE * STEP; // TODO
//		case FILE -> (int) (recording.size() * Constants.bufSize() / width);
//	};
//	audioLength = 0;
//	// zoom ? see TimeDomain
//}

//void scroll(boolean up) { // mouse-wheel  ratio: start to end
//int delta = (up ? -1 : 1) * (int)(.25f * getWidth() * samplesPerPixel);
//// Calculate new start and end positions
//double newStart = Math.max(0, start + delta);
//double newEnd = Math.min(audioLength, end + delta);
//
//// Ensure the new range maintains the current width
//if (newEnd - newStart < range)
//    newStart = newEnd - range;
//
//// Set the new range
//if (newEnd - newStart < FFT_SIZE)
//	return;
//setRange((int) newStart, (int) newEnd);
//rezoom();
//}
//
//void rezoom() {
//zoomCenter = (start + end) / 2;
//}
//
///** absolute sample index */
//void setRange(int begin, int stop) {
//range = stop - begin;
//int max = audioLength - 1;
//if (range > max)
//	range = max;
//if (range < FFT_SIZE)
//	range = FFT_SIZE;
//if (begin < 0) {
//    begin = 0;
//    stop = begin + range; // shift
//}
//if (stop > max) {
//    stop = max;
//    begin = max - range;  // shift
//}
//if (begin == start && stop == end)
//	return;
//start = begin;
//end = stop;
//// TODO:
//samplesPerPixel = (int) ((end - start) / (float)getWidth());
//
//
//rms.generateImage(); // TODO
//spectro.generateImage();
//repaint();
//
//}
//void fullRange() {
//setRange(0, audioLength - 1);
//}
//
////TODO, if fully zoomed out, move zoomCenter
//void setX(float amount) {
//// if 0 = 100% left of zoom center, 100 = 100% right of zoom Center
//// 100% means 1 viewport, 1 whole width in pixels left or right
//int width = range * audioLength;
//int half = (int) (width * 0.5f);
//float justified = amount - 0.5f;
//float factor = justified * 2; // -1 to +1
//int x = (zoomCenter - half) + (int) (width * factor);
//setRange(x, x + width);
//}
//
//void zoom(boolean up) { // ctrl-wheel  ratio: min=all  max=1 atom:?
//setXScale(range * (up ? 0.8f : 1.25f));
//}
//
//void setXScale(float amount) {
//int newSize = (int) (audioLength * amount);
//if (newSize < FFT_SIZE)
//	return; // too small
//int half = (int) (newSize * 0.5f);
//
//rezoom();
//setRange(zoomCenter - half, zoomCenter + half - 1);
//repaint();
//}
//
//void setZoom(int value) {
//setXScale(value * 0.1f);
//// if status == LIVE_ROLLING error
//// if status == LIVE_STOPPED,
//// zooming in on recorded[]
//}

//private void popupMenu(MouseEvent click) {
//JPopupMenu result = new JPopupMenu();
//if (!isLive())
//	result.add(new LambdaMenu("Go Live", e->goLive()));
//result.add(new LambdaMenu("Load", e->load(null)));
//result.add(new LambdaMenu("Save", e->waveform.save()));
//result.add(new LambdaMenu("Save as..", e->waveform.saveAs()));
//result.add(new LambdaMenu("Labels", e->waveform.toggleLables()));
//// result.add(new Freeze);  result.add(Shrink), result.add(Stretch) .add(Flip)
//// result.add(new LambdaMenu("L/R/Stereo", e->System.out.println("Not impleme3nted")));
//result.show(this, click.getX(), click.getY());
//}
//private void verify() {
//correct();
//setTime(caretToMillis());
//repaint();
//}
//void setCaret(float amount) {
//caret = (int) (waveform.size() * amount);
//verify();
//}
//public void setTime(long millis) { // mouse click, drag'n'drop, mouse-wheel
//current = millis;
//now = AudioTools.toSeconds(millis);
//}
///** caret must remain within margin */
//private void correct() {
//if (caret < INSET) caret = INSET;
//if (caret > xMax) caret = xMax;

