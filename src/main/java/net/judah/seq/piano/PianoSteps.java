package net.judah.seq.piano;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
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
import net.judah.seq.track.Edit;
import net.judah.seq.track.Edit.Type;
import net.judah.seq.track.Editor.Delta;
import net.judah.seq.track.Editor.Selection;
import net.judah.seq.track.Editor.TrackListener;
import net.judah.seq.track.MidiTools;
import net.judah.seq.track.NoteTrack;
import net.judah.seq.track.PianoTrack;

public class PianoSteps extends Steps implements TrackListener, MouseMotionListener, MouseListener, Size {

	static final int OFFSET = STEP_WIDTH / 2 - 5;
	private final NoteTrack notes;
	private final JudahClock clock;
	private int width, height;
	private int highlight = -1;

	@Getter private float total;
	@Getter private float unit;

	private Integer on, off;
	private final CCPopup cc;

	// selection state per-row (rows == 2 * clock.getSteps())
	private boolean[] selectedCCs = new boolean[0];
	private boolean[] selectedProg = new boolean[0];

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

		int y;
		for (int i = 0; i < 2 * steps; i++) {
			y = (int)(i * unit);

			if (highlight == i) {
				g.setColor(isBeat(i) ? Pastels.YELLOW.darker() : Pastels.YELLOW);
				g.fillRect(1, y + 1, width - 2, (int) unit - 3);
				g.setColor(Color.BLACK);
			}

			if (cc.getProg(i) != null) {
				if (i >= 0 && i < selectedProg.length && selectedProg[i])
					g.setColor(Pastels.SELECTED);
				else
					g.setColor(Pastels.PROGCHANGE);
				g.fillRect(0, y, width, (int)unit);
			}

			if (ccs[i] > 0) {
				if (i >= 0 && i < selectedCCs.length && selectedCCs[i])
					g.setColor(Pastels.SELECTED);
				else
					g.setColor(Pastels.CC);
				g.fillRect(1, y + 1, width - 2, (int)unit - 2);
				g.setColor(Color.BLACK);
				if (ccs[i] > 1)
					g.drawString("" + ccs[i], OFFSET, y + (int)unit - 3);
			}
			else if (isBeat(i)) {
				int beat = (1 + count / div);
				if (beat > beats) beat -= beats;
				if (i != highlight) {
					g.setColor(Pastels.FADED);
					g.fillRect(0, y, width, (int)unit);
				}
				g.setColor(Color.BLACK);
				g.drawString("" + beat, OFFSET, y + (int)unit - 3);
			}
			else if (count % steps % div == 2) {
				g.drawString("+", OFFSET, y + (int)unit - 3);
			}

			if (cc.getPitch(i) != null) { // backslash means pitchbend present
				g.drawLine(0, y, width, y + (int)unit);
			}

			g.drawLine(0, y + (int)unit, width, y + (int)unit);

			if (++count == steps)
				count = 0;
		}
	}

	public boolean isBeat(int row) {
		return row % clock.getSteps() % clock.getSubdivision() == 0;
	}

	public void highlight(Point p) {
		int replace = p == null ? -1 : (int) ( p.y / (float)height * clock.getSteps() * 2);
		if (replace == highlight)
			return;
		highlight = replace;
		repaint();
	}

	public int toStep(int y) {
		return (int) (total * (y / (float)height));
	}

	/** play notes for given step */
	@Override public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			cc.popup(e, toStep(e.getPoint().y));
			return;
		}
		on = toStep(e.getPoint().y);
	}

	/** play notes for new given step */
	@Override public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e))
			return;
		 off = toStep(e.getPoint().y) + 1;
	}

	/** add actives to track*/
	@Override public void mouseReleased(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e))
			return;
		off = toStep(e.getPoint().y) + 1;
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

	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }

	public void resized(int w, int h) {
		this.width = w;
		this.height = h;
		setMinimumSize(new Dimension(w, h));
		Gui.resize(this, new Dimension(w, h));
		timeSig(clock.getTimeSig());
	}

	@Override public void timeSig(Signature sig) {
		total = 2 * sig.steps;
		unit = height / total;
		// allocate per-row selection arrays
		int rows = 2 * sig.steps;
		selectedCCs = new boolean[rows];
		selectedProg = new boolean[rows];
		repaint();
	}

	@Override
	public void selectionChanged(Selection selection) {
		// Clear previous selection flags
		if (selectedCCs == null || selectedProg == null) {
			// initialize defensively if timeSig not yet called
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

			// Map the event tick into current window then into a row index
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

}
