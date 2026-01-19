package net.judah.seq.piano;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.SwingUtilities;

import judahzone.api.Midi;
import judahzone.api.MidiConstants;
import judahzone.api.Signature;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import lombok.Getter;
import net.judah.gui.Size;
import net.judah.midi.JudahClock;
import net.judah.seq.Steps;
import net.judah.seq.automation.Automation;
import net.judah.seq.automation.CCPopup;
import net.judah.seq.piano.PianoView.Orientation;
import net.judah.seq.track.Edit;
import net.judah.seq.track.Edit.Type;
import net.judah.seq.track.Editor.Delta;
import net.judah.seq.track.Editor.Selection;
import net.judah.seq.track.Editor.TrackListener;
import net.judah.seq.track.MidiTools;
import net.judah.seq.track.NoteTrack;
import net.judah.seq.track.PianoTrack;

public class PianoSteps extends Steps implements TrackListener, Gui.Mouse, Size {

	static final int OFFSET = UNIT / 2 - 5;
	private final NoteTrack notes;
	private final JudahClock clock;
	private int width, height;
	private int highlight = -1;

	@Getter private float total;
	@Getter private float unit;

	private Integer on, off;
	private final CCPopup cc;

	/* selection state per-row (rows == 2 * clock.getSteps()) */
	private boolean[] selectedCCs = new boolean[0];
	private boolean[] selectedProg = new boolean[0];

	private Orientation orientation = Orientation.NOTES_X;

	public PianoSteps(PianoTrack piano, Automation auto) {
		super(piano);
		this.notes = piano;
		this.clock = notes.getClock();
		this.cc = new CCPopup(notes, this, false);
		setLayout(null);
		addMouseMotionListener(this);
		addMouseListener(this);
		piano.getEditor().addListener(this);
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		int[] ccs = cc.populate(notes.getLeft(), notes.getLeft() + notes.getWindow());
		g.drawRect(0, 0, width, height);

		int beats = clock.getTimeSig().beats;
		int steps = clock.getSteps();
		int div = clock.getSubdivision();
		int count = 0;

		if (orientation == Orientation.NOTES_X) {
			paintNotesX(g, ccs, beats, steps, div, count);
		} else {
			paintNotesY(g, ccs, beats, steps, div, count);
		}
	}

	/** Paint for NOTES_X: time progresses vertically (Y-axis) */
	private void paintNotesX(Graphics g, int[] ccs, int beats, int steps, int div, int count) {
		for (int i = 0; i < 2 * steps; i++) {
			int y = (int)(i * unit);
			paintStepRow(g, i, 0, y, width, (int) unit, ccs, beats, steps, div, count);
			if (++count == steps)
				count = 0;
		}
	}

	/** Paint for NOTES_Y: time progresses horizontally (X-axis) */
	private void paintNotesY(Graphics g, int[] ccs, int beats, int steps, int div, int count) {
		for (int i = 0; i < 2 * steps; i++) {
			int x = (int)(i * unit);
			paintStepColumn(g, i, x, 0, (int) unit, height, ccs, beats, steps, div, count);
			if (++count == steps)
				count = 0;
		}
	}

	/** Render a single step row (NOTES_X orientation) */
	private void paintStepRow(Graphics g, int idx, int x, int y, int w, int h,
			int[] ccs, int beats, int steps, int div, int count) {
		if (highlight == idx) {
			g.setColor(isBeat(idx) ? Pastels.YELLOW.darker() : Pastels.YELLOW);
			g.fillRect(1, y + 1, w - 2, h - 3);
			g.setColor(Color.BLACK);
		}

		if (cc.getProg(idx) != null) {
			if (idx >= 0 && idx < selectedProg.length && selectedProg[idx])
				g.setColor(Pastels.SELECTED);
			else
				g.setColor(Pastels.PROGCHANGE);
			g.fillRect(0, y, w, h);
		}

		if (ccs[idx] > 0) {
			if (idx >= 0 && idx < selectedCCs.length && selectedCCs[idx])
				g.setColor(Pastels.SELECTED);
			else
				g.setColor(Pastels.CC);
			g.fillRect(1, y + 1, w - 2, h - 2);
			g.setColor(Color.BLACK);
			if (ccs[idx] > 1)
				g.drawString("" + ccs[idx], OFFSET, y + h - 3);
		}
		else if (isBeat(idx)) {
			int beat = (1 + count / div);
			if (beat > beats) beat -= beats;
			if (idx != highlight) {
				g.setColor(Pastels.FADED);
				g.fillRect(0, y, w, h);
			}
			g.setColor(Color.BLACK);
			g.drawString("" + beat, OFFSET, y + h - 3);
		}
		else if (count % steps % div == 2) {
			g.drawString("+", OFFSET, y + h - 3);
		}

		if (cc.getPitch(idx) != null) {
			g.drawLine(0, y, w, y + h);
		}

		g.drawLine(0, y + h, w, y + h);
	}

	/** Render a single step column (NOTES_Y orientation) */
	private void paintStepColumn(Graphics g, int idx, int x, int y, int w, int h,
			int[] ccs, int beats, int steps, int div, int count) {
		if (highlight == idx) {
			g.setColor(isBeat(idx) ? Pastels.YELLOW.darker() : Pastels.YELLOW);
			g.fillRect(x + 1, 1, w - 3, h - 2);
			g.setColor(Color.BLACK);
		}

		if (cc.getProg(idx) != null) {
			if (idx >= 0 && idx < selectedProg.length && selectedProg[idx])
				g.setColor(Pastels.SELECTED);
			else
				g.setColor(Pastels.PROGCHANGE);
			g.fillRect(x, 0, w, h);
		}

		if (ccs[idx] > 0) {
			if (idx >= 0 && idx < selectedCCs.length && selectedCCs[idx])
				g.setColor(Pastels.SELECTED);
			else
				g.setColor(Pastels.CC);
			g.fillRect(x + 1, 1, w - 2, h - 2);
			g.setColor(Color.BLACK);
			if (ccs[idx] > 1)
				g.drawString("" + ccs[idx], x + (int)(w / 2f) - 4, y + (int)(h / 2f) + 1);
		}
		else if (isBeat(idx)) {
			int beat = (1 + count / div);
			if (beat > beats) beat -= beats;
			if (idx != highlight) {
				g.setColor(Pastels.FADED);
				g.fillRect(x, 0, w, h);
			}
			g.setColor(Color.BLACK);
			g.drawString("" + beat, x + (int)(w / 2f) - 4, y + h - 3);
		}
		else if (count % steps % div == 2) {
			g.drawString("+", x + (int)(w / 2f) - 4, y + h - 3);
		}

		if (cc.getPitch(idx) != null) {
			g.drawLine(x, y, x + w, y + h);
		}

		g.drawLine(x + w, 0, x + w, h);
	}

	public boolean isBeat(int row) {
		return row % clock.getSteps() % clock.getSubdivision() == 0;
	}

	public void highlight(Point p) {
		if (p == null) {
			highlight = -1;
			repaint();
			return;
		}

		int replace;
		if (orientation == Orientation.NOTES_X) {
			replace = (int) (p.y / (float)height * clock.getSteps() * 2);
		} else {
			replace = (int) (p.x / (float)width * clock.getSteps() * 2);
		}

		if (replace == highlight)
			return;
		highlight = replace;
		repaint();
	}

	public int toStep(int coord) {
		if (orientation == Orientation.NOTES_X) {
			return (int) (total * (coord / (float)height));
		} else {
			return (int) (total * (coord / (float)width));
		}
	}

	/** play notes for given step */
	@Override public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			Point p = e.getPoint();
			int coord = (orientation == Orientation.NOTES_X) ? p.y : p.x;
			cc.popup(e, toStep(coord));
			return;
		}
		int coord = (orientation == Orientation.NOTES_X) ? e.getPoint().y : e.getPoint().x;
		on = toStep(coord);
	}

	/** play notes for new given step */
	@Override public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e))
			return;
		int coord = (orientation == Orientation.NOTES_X) ? e.getPoint().y : e.getPoint().x;
		off = toStep(coord) + 1;
	}

	/** add actives to track */
	@Override public void mouseReleased(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e))
			return;
		int coord = (orientation == Orientation.NOTES_X) ? e.getPoint().y : e.getPoint().x;
		off = toStep(coord) + 1;
		if (notes.getActives().isEmpty())
			return;

		int step = notes.getResolution() / clock.getSubdivision();
		int ch = notes.getCh();
		int data2 = (int) (notes.getAmp() * 127f);
		long begin = notes.getLeft() + on * step;
		long end = notes.getLeft() + off * step - 1;
		on = off = null;
		ArrayList<MidiEvent> list = new ArrayList<>();
		for (ShortMessage m : notes.getActives()) {
			list.add(new MidiEvent(Midi.create(MidiConstants.NOTE_ON, ch, m.getData1(), data2), begin));
			list.add(new MidiEvent(Midi.create(MidiConstants.NOTE_OFF, ch, m.getData1(), 127), end));
		}
		notes.getEditor().push(new Edit(Type.NEW, list));
	}

	@Override public void mouseExited(MouseEvent e) {
		highlight(null);
	}

	@Override public void mouseMoved(MouseEvent e) {
		if (on != null)
			highlight(e.getPoint());
	}

	public void resized(int w, int h) {
		this.width = w;
		this.height = h;
		setMinimumSize(new Dimension(w, h));
		Gui.resize(this, new Dimension(w, h));
		timeSig(clock.getTimeSig());
	}

	@Override public void timeSig(Signature sig) {
		total = 2 * sig.steps;
		if (orientation == Orientation.NOTES_X) {
			unit = height / total;
		} else {
			unit = width / total;
		}
		/* allocate per-row selection arrays */
		int rows = 2 * sig.steps;
		selectedCCs = new boolean[rows];
		selectedProg = new boolean[rows];
		repaint();
	}

	@Override
	public void selectionChanged(Selection selection) {
		/* Clear previous selection flags */
		if (selectedCCs == null || selectedProg == null) {
			int rows = 2 * clock.getTimeSig().steps;
			selectedCCs = new boolean[rows];
			selectedProg = new boolean[rows];
		}
		for (int i = 0; i < selectedCCs.length; i++) {
			selectedCCs[i] = false;
			selectedProg[i] = false;
		}
		if (selection == null || selection.events() == null || selection.events().isEmpty()) {
			repaint();
			return;
		}

		long windowStart = notes.getCurrent() * notes.getBarTicks();
		long left = notes.getLeft();
		int stepTicks = notes.getResolution() / clock.getSubdivision();
		int rows = selectedCCs.length;

		for (MidiEvent e : selection.events()) {
			if (!(e.getMessage() instanceof ShortMessage sm)) continue;
			int cmd = sm.getCommand();

			long wrapped = MidiTools.wrapTickInWindow(e.getTick(), windowStart, notes.getWindow());
			long rel = wrapped - left;
			if (rel < 0) continue;
			int idx = (int)(rel / stepTicks);
			if (idx < 0 || idx >= rows) continue;

			if (cmd == Midi.CONTROL_CHANGE) {
				selectedCCs[idx] = true;
			} else if (cmd == Midi.PROGRAM_CHANGE) {
				selectedProg[idx] = true;
			}
		}
		repaint();
	}

	@Override
	public void dataChanged(Delta time) {
		repaint();
	}

	public void setOrientation(Orientation o, int w, int h) {
		height = h;
		width = w;
		this.orientation = o;
		/* recalculate unit based on new time axis dimension */
		if (orientation == Orientation.NOTES_X) {
			unit = height / total;
		} else {
			unit = width / total;
		}
		repaint();
	}

}
