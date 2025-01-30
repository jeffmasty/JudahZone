package net.judah.gui.waves;

import static net.judah.util.Constants.LEFT;
import static net.judah.util.Constants.RIGHT;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.fx.MultiSelect;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.waves.LiveWave.Ears;
import net.judah.gui.waves.LiveWave.LiveWaveData;
import net.judah.gui.waves.Tuner.Tuning;
import net.judah.mixer.Channel;
import net.judah.omni.AudioTools;
import net.judah.omni.Threads;
import net.judah.omni.Zwing;
import net.judah.omni.Zwing.LambdaMenu;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.Memory;
import net.judah.util.RTLogger;


// difficult to focus on start of sample
public class WaveKnobs extends KnobPanel implements MouseListener, MouseWheelListener, MouseMotionListener {

	private static final int S_RATE = Constants.sampleRate();
	private static final int JACK_BUFFER = Constants.bufSize();

	private static final int INSET = 4;
	private static final int MARGIN = INSET * 2;
	private static final int POP_LEFT = 50;
	private static final int TIMELINE = 4 * MARGIN;

	@Getter private final KnobMode knobMode = KnobMode.Wavez;
	private final MultiSelect channels;
	private final Memory mem = JudahZone.getMem();
    @Getter private final Box title = Box.createHorizontalBox();
    @Getter private final JLabel feedback = new JLabel("dat", JLabel.CENTER);
	private int width;
	private int height;
	@Getter private WaveImage waveform;

	@Getter private long current; // caret in millis
	private int xMax;
	private final int cycles = 50;  // mouse wheel scrolls to move through the waveform
	private int caret; // x-pixel // TODO Player
	private Color caretColor = Color.RED;
	private String now = "0.0s";
	private final Tuner tuner = new Tuner();

	private Dimension size = new Dimension(WIDTH_KNOBS, HEIGHT_KNOBS - (STD_HEIGHT + Tuner.TUNER_HEIGHT));

	public WaveKnobs(MultiSelect selected) { // 🜁
		Dimension mine = new Dimension(WIDTH_KNOBS - 10, HEIGHT_KNOBS - STD_HEIGHT);
		Gui.resize(this, mine);
		setSize(mine);
		this.channels = selected;
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(Gui.resize(new JLabel(" "), size));
		Dimension hz = tuner.getSize();
		tuner.setBounds(0, size.height, hz.width, hz.height);
		add(tuner);

		feedback.setFont(Gui.FONT11);
		feedback.setBorder(Gui.SUBTLE);
		title.add(Gui.resize(feedback, new Dimension(KNOB_PANEL.width - MODE_SIZE.width, STD_HEIGHT)));
		Gui.resize(title, new Dimension(size.width - 2 * INSET, STD_HEIGHT));
		goLive();
	}

	public void load(File dotWav) {
		if (dotWav == null) {
			dotWav = Folders.choose();
			if (dotWav == null)
				return;
		}
		try {
			waveform = new FileWave(size, dotWav);
			setDimensions(size);
			feedback.setText(dotWav.getName());
			tuner.setActive(false);
		} catch (IOException e) { RTLogger.warn(this, e); }
	}

	private void goLive() {
		waveform = new LiveWave(size, new Ears(channels, mem));
		setDimensions(size);
		feedback.setText("Live");
		tuner.setActive(true);
	}

	public void setDimensions(Dimension d) {
		width = d.width;
		height = d.height;
		xMax = width - MARGIN;
		verify();
	}

	public boolean isLive() {
		return waveform instanceof LiveWave;
	}

	@Override public boolean doKnob(int idx, int value) {
		if (idx < 0 || idx > 7)
			return false;

		Threads.execute(()->{
			float floater = value * 0.01f;
			switch (idx) {
			    case 4 -> {if (!isLive()) waveform.setX(floater);}
			    case 5 -> waveform.setXScale(1 - floater);
			    case 6 -> waveform.setYScale(floater);
			    case 7 -> waveform.setIntensity(floater);
			    default -> { return; }
			}
			repaint();
		});
		return true;
	}

	private void verify() {
		correct();
		setTime(caretToMillis());
		repaint();
	}

	@Override
	public void paintComponent(final Graphics g) {
		super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        //performance? g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // draw already generated waveform
        g.drawImage(waveform, INSET, INSET, null);
        drawZoomCenter(g2d);
        drawCaret(g2d); // TODO audio player

    }

	private void drawZoomCenter(Graphics2D g) {
		if (!waveform.isPrintabels())
			return;
		int zoomCenter = waveform.pixelOf(waveform.getZoomCenter());
		if (zoomCenter < 0)
			return;
		g.setStroke(Zwing.DASHED_LINE);
		g.setColor(Pastels.MY_GRAY);
		g.drawLine(zoomCenter, INSET, zoomCenter, height - INSET);
	}

	private void drawCaret(Graphics2D g) {
		if (!waveform.inWindow(current))
			return;
        // Draw caret
        g.setColor(caretColor);
        g.fillOval(caret - 2, getHeight() - MARGIN, MARGIN, MARGIN);
        g.setStroke(Zwing.DASHED_LINE);
        g.drawLine(caret + 2, INSET, caret + 2, height);
        // right justify now string if off right edge
        int nowWidth = g.getFontMetrics().stringWidth(now);
        if (caret + nowWidth + MARGIN < width)
        	g.drawString(now, caret + MARGIN, TIMELINE);
        else //
        	g.drawString(now, caret - POP_LEFT, TIMELINE);
	}

	void setCaret(float amount) {
		caret = (int) (waveform.getRecording().size() * amount);
		verify();
	}

	public void setTime(long millis) { // mouse click, drag'n'drop, mouse-wheel
		current = millis;
		now = AudioTools.toSeconds(millis);
	}

    /** caret must remain within margin */
	private void correct() {
	    if (caret < INSET) caret = INSET;
	    if (caret > xMax) caret = xMax;
	}

	public long sampleToMillis(long samplePosition) {
		return (long) ((samplePosition / (float)S_RATE) * 1000f);
	}

	public long caretToMillis() { // no zoom
		return sampleToMillis(caret * waveform.getSamplesPerPixel());
	}

	public int caretToFrame() {
		return caret * waveform.getSamplesPerPixel() / JACK_BUFFER;
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
        	waveform.attenuate(up);
        	repaint();
        }
        else if (isCtrlPressed) {
        	waveform.zoom(up);
        	repaint();
        } else if (isShiftPressed) { // shift caret left or right
    		int incrementFactor = width / cycles;
    		caret += up ? -incrementFactor : incrementFactor;
    		// TODO also scroll/left right instead of correct();
    	    verify();
        }
        else { //no modifiers = scroll left or right
        	waveform.scroll(up); // scroll left/right
        	repaint();
        }
	}

	@Override public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			caret = e.getX();
			verify();
		}
		else if (e.getButton() == MouseEvent.BUTTON3)
			popupMenu(e);
	}

	@Override public void mouseDragged(MouseEvent e) { // TODO drag x, not caret
		caret = e.getX();
		verify();
	}

	@Override public void mouseMoved(MouseEvent e) {	}
	@Override public void mouseClicked(MouseEvent e) {	 }
	@Override public void mouseReleased(MouseEvent e) {	 }
	@Override public void mouseEntered(MouseEvent e) {	}
	@Override public void mouseExited(MouseEvent e) {	}

	private void popupMenu(MouseEvent click) {
		JPopupMenu result = new JPopupMenu();
		if (!isLive())
			result.add(new LambdaMenu("Go Live", e->goLive()));
		result.add(new LambdaMenu("Load", e->load(null)));
		result.add(new LambdaMenu("Save", e->waveform.save()));
		result.add(new LambdaMenu("Save as..", e->waveform.saveAs()));
		result.add(new LambdaMenu("Labels", e->waveform.toggleLables()));
		// result.add(new Freeze);  result.add(Shrink), result.add(Stretch) .add(Flip)
		// result.add(new LambdaMenu("L/R/Stereo", e->System.out.println("Not impleme3nted")));
		result.show(this, click.getX(), click.getY());
	}

	public void process() {
		if (waveform instanceof LiveWave || tuner.isActive()) {
			float[][] buf = mem.getFrame();
			channels.forEach(ch->copy(ch, buf));
			if (waveform instanceof LiveWave live)
				MainFrame.update(new LiveWaveData(live, buf));
			if (tuner.isActive())
				MainFrame.update(new Tuning(tuner, buf));
		}
	}

	private void copy(Channel ch, float[][] out) {
		AudioTools.mix(ch.getLeft(), out[LEFT]);
		AudioTools.mix(ch.getRight(), out[RIGHT]);
	}


	@Override
	public void update() {

	}

	@Override
	public void pad1() {
		tuner.setActive(!tuner.isActive());

	}


}
