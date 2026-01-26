package net.judah.seq.piano;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;

import judahzone.api.Midi;
import judahzone.api.Signature;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.util.RTLogger;
import lombok.Getter;
import net.judah.seq.piano.PianoView.Orientation;
import net.judah.seq.track.Edit;
import net.judah.seq.track.Edit.Type;
import net.judah.seq.track.Editor.Selection;
import net.judah.seq.track.MidiNote;
import net.judah.seq.track.MidiTools;
import net.judah.seq.track.MusicBox;
import net.judah.seq.track.NotePairer;
import net.judah.seq.track.Prototype;

/** Display midi music in piano grid with orientation support (NOTES_X, NOTES_Y). */
public class Piano extends MusicBox {

	private final PianoKeys piano;
	private final PianoSteps steps;
	@Getter private final PianoView view;

	private Orientation orientation;
	private float ratio;
	private float unit;
	private boolean pressOnSelected = false;

	public Piano(PianoView view, PianoSteps currentBeat, PianoKeys roll) {
	    super(view.track);
	    this.view = view;
	    this.steps = currentBeat;
	    this.piano = roll;
	    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}

	@Override
	public void selectionChanged(Selection selection) {
		if (selection == null || selection.originId() == this) {
			return; // Ignore our own events
		}
		selected.clear();
		if (selection.events() != null) {
			selected.addAll(selection.events());
		}
		repaint();
	}

	/** Convert screen coordinate to MIDI tick (orientation-aware). */
	@Override
	public long toTick(Point p) {
		float coord = (orientation == Orientation.NOTES_X) ? p.y : p.x;
		float dimension = (orientation == Orientation.NOTES_X) ? height : width;
		return (long) (coord / dimension * track.getWindow() + track.getLeft());
	}

	/** Convert screen coordinate to MIDI note (account for orientation + octaves/tonic). */
	@Override public int toData1(Point p) {
		if (p == null) return 0;
		if (orientation == Orientation.NOTES_X) {
			float coord = p.x;
			float scaledWidth = view.scaledWidth;
			int key = (int) (coord / scaledWidth) + view.tonic;
			if (key < 0) return 0; if (key > 127) return 127; return key; }
		else { // NOTES_Y: vertical axis maps pitch, with high notes at top.
			// Compute index from top and invert to get MIDI note.
			float noteHeight = height / (float) (view.range + 1);
			if (noteHeight <= 0) return view.tonic;
			int indexFromTop = (int) (p.y / noteHeight);
			int top = view.tonic + view.range;
			int key = top - indexFromTop;
			if (key < 0) return 0;
			if (key > 127) return 127;
			return key;
		}
	}

	/** Check if step index is a beat marker. */
	public boolean isBar(int row) {
	    return row % clock.getSteps() == 0;
	}

	@Override
	public void paint(Graphics g) {
	    super.paint(g);
	    g.setColor(Pastels.FADED);

	    if (orientation == Orientation.NOTES_X) {
	        paintNotesX(g);
	    } else {
	        paintNotesY(g);
	    }
	}

	/** Paint grid for NOTES_X: notes horizontal (X), time vertical (Y). */
	private void paintNotesX(Graphics g) {
		float noteWidth = view.scaledWidth;
		int keyWidth = (int) noteWidth;
		int tonic = view.tonic;
		int range = view.range + 1;
		int top = tonic + range;

		// Draw vertical lines for each note
		for (int i = 0; i <= range; i++) {
			int x = (int) (i * noteWidth);
			if (piano.isLabelC(tonic + i)) {
				g.setColor(Pastels.BLUE);
				g.fillRect(x, 0, keyWidth, height);
				g.setColor(Pastels.FADED);
			}
			g.drawLine(x, 0, x, height);
		}

		// Draw horizontal lines for each step
		for (int i = 0; i < 2 * clock.getSteps(); i++) {
			int y = (int) (i * unit);
			if (steps.isBeat(i))
				g.fillRect(0, y, width, (int) unit);
			else
				g.drawLine(0, (int) (y + unit), width, (int) (y + unit));
			if (isBar(i)) {
				g.setColor(Color.GRAY);
				g.drawLine(0, y, width, y);
				g.setColor(Pastels.FADED);
			}
		}

		// Paint notes
		paintNotesForOrientation(g, tonic, top, noteWidth, keyWidth, false);
	}

	/** Paint grid for NOTES_Y: notes vertical (Y), time horizontal (X). */
	private void paintNotesY(Graphics g) {
		float noteHeight = height / (float) (view.range + 1);
		int keyHeight = (int) noteHeight;
		int tonic = view.tonic;
		int range = view.range + 1;
		int top = tonic + range;

		// Draw horizontal lines for each note (from top=high to bottom=low)
		for (int i = 0; i <= range; i++) {
			int y = (int) (i * noteHeight);
			int noteNum = top - i; // Reverse: high notes at top
			if (piano.isLabelC(noteNum)) {
				g.setColor(Pastels.BLUE);
				g.fillRect(0, y, width, keyHeight);
				g.setColor(Pastels.FADED);
			}
			g.drawLine(0, y, width, y);
		}

		// Draw vertical lines for each step
		for (int i = 0; i < 2 * clock.getSteps(); i++) {
			int x = (int) (i * unit);
			if (steps.isBeat(i))
				g.fillRect(x, 0, (int) unit, height);
			else
				g.drawLine((int) (x + unit), 0, (int) (x + unit), height);
			if (isBar(i)) {
				g.setColor(Color.GRAY);
				g.drawLine(x, 0, x, height);
				g.setColor(Pastels.FADED);
			}
		}

		// Paint notes
		paintNotesForOrientation(g, tonic, top, noteHeight, keyHeight, true);
	}

	/** Paint note events (shared logic, orientation-aware). */
	private void paintNotesForOrientation(Graphics g, int tonic, int top,
			float noteDim, int noteSize, boolean isNotesY) {
		int x, y, width_rect, height_rect;

		for (MidiNote p : scroll.populate()) {
			if (!(p.getMessage() instanceof ShortMessage s)) continue;
			int data1 = s.getData1();
			if (data1 < tonic || data1 > top) continue;

			boolean isNoteSelected = isSelectedOn(p);
			boolean isNoteInDrag = dragging != null && isNoteInDraggingList(p);
			if (isNoteInDrag) continue;

			long tickOffset = p.getTick() - track.getLeft();
			long duration = (p.getOff() != null) ? (p.getOff().getTick() - p.getTick()) : track.getStepTicks();

			if (isNotesY) {
				// NOTES_Y: x=time, y=pitch (inverted from top)
				x = (int) (tickOffset * ratio);
				y = (int) ((top - data1) * noteDim);
				width_rect = (int) (duration * ratio);
				height_rect = noteSize;
			} else {
				// NOTES_X: x=pitch, y=time
				x = (int) (noteDim * (data1 - tonic));
				y = (int) (tickOffset * ratio);
				width_rect = noteSize;
				height_rect = (int) (duration * ratio);
			}

			g.setColor(isNoteSelected ? highlightColor(s.getData2()) : velocityColor(s.getData2()));
			g.fill3DRect(x, y, width_rect, height_rect, true);
		}

		// Paint dragging preview
		if (dragging != null) {
			Point mouse = getMousePosition();
			if (mouse == null) mouse = drag;
			if (mouse == null) return;

			Prototype now = new Prototype(toData1(mouse), track.quantize(toTick(mouse)));
			int data1Delta = now.data1 - click.data1;
			long tickDelta = now.tick - click.tick;

			g.setColor(Pastels.ORANGE);

			for (MidiEvent e : dragging) {
				if (!(e.getMessage() instanceof ShortMessage sm) || !Midi.isNoteOn(sm)) continue;

				int originalData1 = sm.getData1();
				long originalTick = e.getTick();
				long noteEnd = -1;

				if (e instanceof MidiNote mn && mn.getOff() != null) {
					noteEnd = mn.getOff().getTick();
				} else {
					MidiEvent off = NotePairer.getOff(e, t);
					if (off != null) noteEnd = off.getTick();
				}
				if (noteEnd == -1) noteEnd = originalTick + track.getStepTicks();

				int newKey = originalData1 + data1Delta;
				if (newKey < 0) newKey = 0;
				if (newKey > 127) newKey = 127;

				long newTick = originalTick + tickDelta;
				long newEndTick = noteEnd + tickDelta;

				long tickOffset = newTick - track.getLeft();
				long duration = newEndTick - newTick;

				if (isNotesY) {
					x = (int) (tickOffset * ratio);
					y = (int) ((top - newKey) * noteDim);
					width_rect = (int) (duration * ratio);
					height_rect = noteSize;
				} else {
					x = (int) (noteDim * (newKey - tonic));
					y = (int) (tickOffset * ratio);
					width_rect = noteSize;
					height_rect = (int) (duration * ratio);
				}

				g.fill3DRect(x, y, width_rect, height_rect, true);
			}
		}

		// Paint selection shade
		if (mode == DragMode.SELECT || mode == DragMode.CREATE) {
			Graphics2D g2d = (Graphics2D) g;
			Composite original = g2d.getComposite();
			g2d.setComposite(transparent);
			g2d.setPaint(mode == DragMode.SELECT ? Pastels.BLUE : Pastels.ORANGE);
			Point mouse = MouseInfo.getPointerInfo().getLocation();
			SwingUtilities.convertPointFromScreen(mouse, this);
			g2d.fill(shadeRect(mouse.x, mouse.y));
			g2d.setComposite(original);
		}
	}

	@Override
	public void timeSig(Signature sig) {
	    unit = (orientation == Orientation.NOTES_X) ?
			height / (2f * sig.steps) : width / (2f * sig.steps);
	    ratio = (orientation == Orientation.NOTES_X) ?
			height / (2f * track.getBarTicks()) : width / (2f * track.getBarTicks());
	    repaint();
	}

	/** Precompute units based on current orientation and dimensions. */
	public void calculateUnits() {
		if (orientation == Orientation.NOTES_X) {
			// NOTES_X: notes horizontal (X), time vertical (Y)
			unit = height / (2f * clock.getSteps());
			ratio = height / (2f * track.getBarTicks());
		} else {
			// NOTES_Y: notes vertical (Y), time horizontal (X)
			unit = width / (2f * clock.getSteps());
			ratio = width / (2f * track.getBarTicks());
		}
	}

	@Override
	public void resized(int w, int h) {
	    width = w;
	    height = h;
	    setMinimumSize(new Dimension(w, h));
	    Gui.resize(this, new Dimension(width, height));
	    timeSig(clock.getTimeSig());
	}

	@Override
	public void mouseExited(MouseEvent e) {
	    piano.highlight(-1);
	    steps.highlight(null);
	}

	@Override
	public void mouseMoved(MouseEvent mouse) {
	    Point p = mouse.getPoint();
	    piano.highlight(toData1(p));
	    steps.highlight(p);
	}

	@Override
	public void mousePressed(MouseEvent mouse) {
	    pressOnSelected = false;
	    if (SwingUtilities.isRightMouseButton(mouse)) return;

	    Prototype dat = translate(mouse.getPoint());
	    MidiEvent existing = noteAt(dat);

	    if (mouse.isShiftDown()) {
	        drag = mouse.getPoint();
	        mode = DragMode.SELECT;
	    } else if (existing == null) {
	        click = new Prototype(dat.data1, track.quantize(dat.tick));
	        drag = mouse.getPoint();
	        mode = DragMode.CREATE;
	    } else {
			boolean noteIsSelected = isSelectedOn(existing);
	        if (mouse.isControlDown()) {
	            if (noteIsSelected) removeSelectionForOn(existing);
	            else addSelectionForOn(existing);
	        } else {
				if (!noteIsSelected) select(getNoteOnAndOff(existing));
	            pressOnSelected = true;
				click = dat;
	        }
	    }
	    repaint();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	    if (pressOnSelected && mode == null) {
	        dragStart(e.getPoint());
	        pressOnSelected = false;
	    }
	    if (mode != null) {
	        drag = e.getPoint();
	        repaint();
	    }
	}

	private MidiEvent noteAt(Prototype proto) {
	    MidiEvent exact = MidiTools.lookup(NOTE_ON, proto.data1, track.quantize(proto.tick), t);
	    if (exact != null) return exact;

	    for (int i = 0; i < t.size(); i++) {
	        MidiEvent e = t.get(i);
	        if (!(e.getMessage() instanceof ShortMessage sm) || !Midi.isNoteOn(sm)) continue;
	        if (sm.getData1() != proto.data1) continue;

	        long noteStart = e.getTick();
	        if (noteStart > proto.tick) break;

	        MidiEvent off = NotePairer.getOff(e, t);
	        long noteEnd = off != null ? off.getTick() : noteStart + track.getStepTicks();
	        if (proto.tick >= noteStart && proto.tick < noteEnd) return e;
	    }
	    return null;
	}

	@Override
	public void mouseReleased(MouseEvent mouse) {
	    if (mode != null) {
	        switch(mode) {
	        case CREATE:
	            try {
					Point releasePoint = mouse.getPoint();
	                long left = click.tick;
	                long right = toTick(releasePoint);
	                if (left > right) {
	                    long temp = left; left = right; right = temp;
	                }
					right = track.quantizePlus(right);
					if (right <= left) right = left + track.getStepTicks();

	                int data1 = click.data1;
	                MidiEvent on = Midi.createEvent(left, NOTE_ON, track.getCh(), data1, 100);
	                MidiEvent off = Midi.createEvent(right, NOTE_OFF, track.getCh(), data1, 0);

	                ArrayList<MidiEvent> added = new ArrayList<>();
	                added.add(on);
	                added.add(off);
	                track.getEditor().push(new Edit(Type.NEW, added));
	            } catch (Exception e) {
	                RTLogger.warn(this, e);
	            }
	            break;

	        case SELECT:
	            if (drag == null) break;
	            Prototype a = translate(drag);
	            Prototype b = translate(mouse.getPoint());
	            long leftTick = Math.min(a.tick, b.tick);
	            long rightTick = Math.max(a.tick, b.tick);
	            int lowData1 = Math.min(a.data1, b.data1);
	            int highData1 = Math.max(a.data1, b.data1);

	            List<MidiEvent> newSel = track.getEditor().getEventsInArea(leftTick, rightTick, lowData1, highData1);
	            if (mouse.isControlDown()) {
	                LinkedHashSet<MidiEvent> merged = new LinkedHashSet<>(selected);
	                merged.addAll(newSel);
	                select(new ArrayList<>(merged));
	            } else {
	                select(newSel);
	            }
	            break;

	        case TRANSLATE:
	            drop(mouse.getPoint());
	            break;
	        }
	    }
	    mode = null;
	    click = null;
	    drag = null;
	    dragging = null;
	    pressOnSelected = false;
	    repaint();
	}

	@Override
	public void dragStart(Point mouse) {
	    mode = DragMode.TRANSLATE;
		drag = mouse;
	    dragging = new ArrayList<>();
		for (MidiEvent sel : selected) {
			if (Midi.isNoteOn(sel.getMessage())) {
				MidiEvent off = NotePairer.getOff(sel, t);
				dragging.add(new MidiNote(sel, off));
			}
		}
	}

	@Override
	public void drag(Point mouse) {
		// Visual updates handled in paint() based on current mouse position
	}

	@Override
	public void drop(Point mouse) {
		if (click == null) return;
	    ArrayList<MidiEvent> raw = buildInterleavedForSelected();

	    Edit e = new Edit(Type.TRANS, raw);
	    Prototype now = translate(mouse);

		long tickDelta = now.tick - click.tick;
		long stepDelta = tickDelta / track.getStepTicks();

	    Prototype destination = new Prototype(now.data1 - click.data1, stepDelta);
	    e.setDestination(destination, click);
	    track.getEditor().push(e);
	}

	private ArrayList<MidiEvent> buildInterleavedForSelected() {
	    ArrayList<MidiEvent> raw = new ArrayList<>();
	    for (MidiEvent e : selected) {
	        if (Midi.isNoteOn(e.getMessage())) {
	            raw.add(e);
	            MidiEvent off = NotePairer.getOff(e, t);
	            if (off != null) raw.add(off);
	        }
	    }
	    return raw;
	}

    private List<MidiEvent> getNoteOnAndOff(MidiEvent on) {
        ArrayList<MidiEvent> pair = new ArrayList<>();
        if (on != null && Midi.isNoteOn(on.getMessage())) {
            pair.add(on);
            MidiEvent off = NotePairer.getOff(on, t);
            if (off != null) pair.add(off);
        }
        return pair;
    }

	private boolean isNoteInDraggingList(MidiNote note) {
		if (dragging == null || note == null) return false;
		for (MidiEvent draggedEvent : dragging) {
			if (draggedEvent.getTick() == note.getTick() &&
				draggedEvent.getMessage() instanceof ShortMessage smd &&
				note.getMessage() instanceof ShortMessage smn &&
				smd.getData1() == smn.getData1())
				return true;
		}
		return false;
	}

	private boolean isSelectedOn(MidiEvent on) {
	    if (on == null || on.getMessage() == null || selected.isEmpty()) return false;
	    return selected.stream().anyMatch(sel ->
			sel.getTick() == on.getTick() &&
			MidiTools.messagesMatch(sel.getMessage(), on.getMessage()));
	}

	private void addSelectionForOn(MidiEvent on) {
	    List<MidiEvent> copy = new ArrayList<>(selected);
	    if (!isSelectedOn(on)) {
	        copy.addAll(getNoteOnAndOff(on));
	        select(copy);
	    }
	}

	private void removeSelectionForOn(MidiEvent on) {
	    if (selected.isEmpty() || on == null) return;
	    List<MidiEvent> toRemove = getNoteOnAndOff(on);
	    List<MidiEvent> copy = new ArrayList<>(selected);
	    copy.removeAll(toRemove);
	    select(copy);
	}

	/** Update orientation and recalculate metrics. */
	public void setOrientation(Orientation o, int w, int h) {
		this.orientation = o;
		this.width = w;
		this.height = h;
		timeSig(clock.getTimeSig());
		repaint();
	}
}
