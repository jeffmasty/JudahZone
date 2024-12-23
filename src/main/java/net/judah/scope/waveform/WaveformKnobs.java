package net.judah.scope.waveform;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;

import javax.swing.JPopupMenu;

import lombok.Getter;
import net.judah.omni.AudioTools;
import net.judah.omni.Recording;
import net.judah.omni.Threads;
import net.judah.omni.WavConstants;
import net.judah.omni.Zwing;
import net.judah.scope.ScopeView;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

// on screen menu?   zoom, caret, factor
// on screen feedback   amplitude-scale  caret  window-scale
// on screen graph:  redline time   window   amplitude scale
// left/right arrow keys=frame  ctrl-left/right = 10%   home/end  pageUp/Down
public class WaveformKnobs extends ScopeView implements MouseListener, MouseWheelListener, MouseMotionListener,
		ComponentListener, WavConstants, Zwing {

    @Getter float zoom;
	@Getter private Color timeColor = Color.RED;

	private static final int INSET = 4;
	private static final int MARGIN = INSET * 2;
	private static final int TIMELINE = 2 * MARGIN;
    private static final int POP_LEFT = 50;
    private static final int ATOMIC_SIZE = FFT_WINDOW + 2 * FFT_WINDOW; // 6 frames

    public static final int MAX_LIVE = 2 ^ 8;
    private final Recording floats = new Recording();


	@Getter private JudahWave waveform;
	private File file = new File("waveform.png");
	private int width;
	private int height;
	private Dimension imgSize;
	private int caret; // x-pixel
	@Getter private long current; // caret in millis
	private String now = "0.0s";

	private final int cycles = 50;  // mouse wheel scrolls to move through the waveform
	private int xMax;

	public WaveformKnobs(Dimension size) {
		setLayout(null);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addComponentListener(this);
		setDimensions(size);

	}

	public void setDimensions(Dimension d) {
		width = d.width;
		height = d.height;
		xMax = width - MARGIN;
		imgSize = new Dimension(new Dimension(width - MARGIN, height - MARGIN));
		try {
			waveform = new JudahWave(imgSize);
			redo();
		} catch (Exception e) {RTLogger.warn(this, e);}
	}

	@Override
	public void paintComponent(final Graphics g) {
        Graphics2D g2d = (Graphics2D)g;

        //performance? g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // generated waveform
        g.drawImage(waveform, INSET, INSET, null);
        // draw grid stuff
        g.setColor(Color.BLACK);
        int slider = getHeight() - INSET;
        g.drawLine(INSET, slider, getWidth() - INSET, slider);

        if (inWindow(current)) {
            // Draw caret
            g.setColor(timeColor);
            g.fillOval(caret - 2, getHeight() - MARGIN, MARGIN, MARGIN);
            g2d.setStroke(DASHED_LINE);
            g2d.drawLine(caret + 2, INSET, caret + 2, height);
            // right justify now string if off right edge
            int nowWidth = g2d.getFontMetrics().stringWidth(now);
            if (caret + nowWidth + MARGIN < width)
            	g2d.drawString(now, caret + MARGIN, TIMELINE);
            else //
            	g2d.drawString(now, caret - POP_LEFT, TIMELINE);
        }

    }


	public void setTime(long millis) { // mouse click, drag'n'drop, mouse-wheel
		current = millis;
		now = AudioTools.toSeconds(millis);
	}

	private void popupMenu(MouseEvent click) {
		JPopupMenu result = new JPopupMenu();
		result.add(new LambdaMenu("Load", e->load()));
		result.add(new LambdaMenu("Save", e->waveform.save(file)));
		result.add(new LambdaMenu("Save as..", e->waveform.saveAs()));
		result.add(new LambdaMenu("L/R/Stereo", e->System.out.println("Not impleme3nted")));
		result.add(new LambdaMenu("Labels", e->System.out.println("Not impleme3nted")));
		result.show(this, click.getX(), click.getY());
	}

	private void load() {
		File file = Folders.choose();
		if (file == null)
			return;
		Threads.execute(() ->{
			try {
				waveform = new JudahWave(file, imgSize);
				repaint();
			} catch (Exception e) { RTLogger.warn(this, e); }
		});
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
        	waveform.intensity(up);
        } else if (isCtrlPressed) {
        	waveform.zoom(up);
        } else if (isShiftPressed) {
        	waveform.scroll(up); // scroll left/right
        } else { // no modifiers = set x-axis (time) position
    		int incrementFactor = width / cycles;
    		caret(caret + incrementFactor * (up ? -1 : 1));
        }

        redo();
	}


	@Override public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			caret = e.getX();
			redo();
		}
		else if (e.getButton() == MouseEvent.BUTTON3) {
			popupMenu(e);
		}
	}

	@Override public void mouseDragged(MouseEvent e) {
		caret = e.getX();
		redo();
	}

    /** caret must remain within margin */
	private void correct() {
	    if (caret < INSET) caret = INSET;
	    if (caret > xMax) caret = xMax;
	}

	private void redo() {

		correct();
		setTime(caretToMillis());
		repaint();
	}

	public long caretToMillis() { // no zoom
		long samplePosition = caret * waveform.getSamplesPerPixel();
		return (long) ((samplePosition / (float)S_RATE) * 1000f);
	}

	public int caretToAtom() {
		return caret * waveform.getSamplesPerPixel() / ATOMIC_SIZE;
	}

	public int caretToBuffer() {
		return caret * waveform.getSamplesPerPixel() / JACK_BUFFER;
	}

	@Override
	public void componentResized(ComponentEvent e) {
		setDimensions(new Dimension(getWidth(), getHeight()));
	}

	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mouseReleased(MouseEvent e) { }

	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseMoved(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }

	@Override public void componentShown(ComponentEvent e) {
		RTLogger.log(this, "WaveKnobs Shown");
	}
	@Override public void componentHidden(ComponentEvent e) { }
	@Override public void componentMoved(ComponentEvent e) { }

	public void setTimeLineColor(Color c) {
		timeColor = c;
		repaint();
	}

	@Override
	public void process(float[][] stereo) {
		waveform.process(stereo);
	}

	@Override
	public void knob(int idx, int value) {
		float percent = value * 0.01f;
		switch (idx) {
			case 4: // caret
				caret(percent);
				break;
			case 5: // scroll
				waveform.scroll(percent);
				break;
			case 6: // range
				waveform.zoom(percent);
				break;
			case 7: // attenuate gain
				waveform.intensity(value);
				break;
			default: return;
		}
		redo();
	}

	public boolean inWindow(long millis) {
		// TODO
		return false;
	}

	public void caret(float percent) {
		// TODO also scroll/left right instead of correct()?

	}

}