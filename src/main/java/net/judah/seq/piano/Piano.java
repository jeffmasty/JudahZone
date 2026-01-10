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
import net.judah.seq.track.Edit;
import net.judah.seq.track.MidiNote;
import net.judah.seq.track.MidiTools;
import net.judah.seq.track.MusicBox;
import net.judah.seq.track.NotePairer;
import net.judah.seq.track.Prototype;
import net.judah.seq.track.Edit.Type;
import net.judah.seq.track.Editor.Selection;

/* display midi music in piano grid*/
public class Piano extends MusicBox {

	private final PianoKeys piano;
	private final PianoSteps steps;
	private final PianoView zoom;

	private float ratio;
	private float unit;
	private boolean pressOnSelected = false;

	public Piano(PianoView view, PianoSteps currentBeat, PianoKeys roll) {
	    super(view.track);
	    this.zoom = view;
	    this.steps = currentBeat;
	    this.piano = roll;
	    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}

	@Override
	public void selectionChanged(Selection selection) {
		if (selection == null || selection.originId() == this) {
			return; // Ignore our own events
		}
		// Update internal selection from external source
		selected.clear();
		if (selection.events() != null) {
			selected.addAll(selection.events());
		}
		repaint();
	}

	@Override public long toTick(Point p) {
	    return (long) (p.y / (float)(height) * track.getWindow() + track.getLeft());
	}

	@Override public int toData1(Point p) {
		int key = (int) (p.x / zoom.scaledWidth) + zoom.tonic;
		if (key < 0) return 0;
		if (key > 127) return 127;
		return key;
	}

	public boolean isBar(int row) {
	    return row % clock.getSteps() == 0;
	}

	@Override public void paint(Graphics g) {
	    super.paint(g);
	    g.setColor(Pastels.FADED);

	    float noteWidth = zoom.scaledWidth;
	    int keyWidth = (int)noteWidth;
	    int tonic = zoom.tonic;
	    int range = zoom.range + 1;
	    int top = tonic + range;
	    int x; // columns (notes)
	    for (int i = 0; i <= range; i++) {
	        x = (int) (i * noteWidth);
	        if (piano.isLabelC(tonic + i)) {
	            g.setColor(Pastels.BLUE);
	            g.fillRect(x, 0, keyWidth, height);
	            g.setColor(Pastels.FADED);
	        }
	        g.drawLine(x, 0, x, height);
	    }

	    int y; // rows (steps)
	    for (int i = 0; i < 2 * clock.getSteps(); i++) {
	        y = (int) (i * unit);
	        if (steps.isBeat(i))
	            g.fillRect(0, y, width, (int) unit);
	        else
	            g.drawLine(0, (int)(y + unit), width, (int)(y + unit));
	        if (isBar(i)) {
	            g.setColor(Color.GRAY);
	            g.drawLine(0, y, width, y);
	            g.setColor(Pastels.FADED);
	        }
	    }

	    // Paint notes from the track
	    int yheight, data1;
	    for (MidiNote p : scroll.populate()) {
	        if (p.getMessage() instanceof ShortMessage s) {
	            data1 = s.getData1();
	            if (data1 < tonic || data1 > top) continue;

	            x = (int) (noteWidth * (data1 - tonic));
	            y = (int) ((p.getTick() - track.getLeft()) * ratio);
	            yheight = (p.getOff() != null) ? (int) ((p.getOff().getTick() - p.getTick()) * ratio) : keyWidth;

	            boolean isNoteSelected = isSelectedOn(p);
				boolean isNoteInDrag = dragging != null && isNoteInDraggingList(p);

				// Do not draw the original note if it's being dragged
				if (isNoteInDrag) continue;

				g.setColor(isNoteSelected ? highlightColor(s.getData2()) : velocityColor(s.getData2()));
	            g.fill3DRect(x, y, keyWidth, yheight, true);
	        }
	    }

		// Paint the visual representation of notes being dragged
	    if (dragging != null) {
			Point mouse = getMousePosition();
			if (mouse == null) { // If mouse is outside component, use last known drag point
				mouse = drag;
			}
			Prototype now = new Prototype(toData1(mouse), track.quantize(toTick(mouse)));
			int data1Delta = now.data1 - click.data1;
			long tickDelta = now.tick - click.tick;

			g.setColor(Pastels.ORANGE);

	        for (MidiEvent e : dragging) {
				if (!(e.getMessage() instanceof ShortMessage sm) || !Midi.isNoteOn(sm)) continue;

				long originalTick = e.getTick();
				int originalData1 = sm.getData1();
				long noteEnd = -1;

				if (e instanceof MidiNote mn && mn.getOff() != null) {
					noteEnd = mn.getOff().getTick();
				} else {
					MidiEvent off = NotePairer.getOff(e, t);
					if (off != null) noteEnd = off.getTick();
				}
				if (noteEnd == -1) noteEnd = originalTick + track.getStepTicks();


				int newKey = originalData1 + data1Delta;
				if (newKey < 0) newKey = 0; if (newKey > 127) newKey = 127;

				long newTick = originalTick + tickDelta;
				long newEndTick = noteEnd + tickDelta;


	            x = (int) (noteWidth * (newKey - tonic));
	            y = (int) ((newTick - track.getLeft()) * ratio);
				yheight = (int) ((newEndTick - newTick) * ratio);

	            g.fill3DRect(x, y, keyWidth, yheight, true);
	        }
	    }

	    if (mode == DragMode.SELECT || mode == DragMode.CREATE) {
	        // highlight drag region
	        Graphics2D g2d = (Graphics2D)g;
	        Composite original = g2d.getComposite();
	        g2d.setComposite(transparent);
	        g2d.setPaint(mode == DragMode.SELECT ? Pastels.BLUE : Pastels.ORANGE);
	        Point mouse = MouseInfo.getPointerInfo().getLocation();
			SwingUtilities.convertPointFromScreen(mouse, this);
	        g2d.fill(shadeRect(mouse.x, mouse.y));
	        g2d.setComposite(original);
	    }
	}

	@Override public void timeSig(Signature sig) {
	    unit = height / (2f * sig.steps);
	    ratio = height / (2f * track.getBarTicks());
	    repaint();
	}

	@Override
	public void resized(int w, int h) {
	    width = w;
	    height = h;
	    setMinimumSize(new Dimension(w, h));

	    Gui.resize(this, new Dimension(width, height));
	    timeSig(clock.getTimeSig());
	}

	@Override public void mouseExited(MouseEvent e) {
	    piano.highlight(-1);
	    steps.highlight(null);
	}

	@Override public void mouseMoved(MouseEvent mouse) {
	    Point p = mouse.getPoint();
	    piano.highlight(toData1(p));
	    steps.highlight(p);
	}

	@Override public void mousePressed(MouseEvent mouse) {
	    pressOnSelected = false; // reset
	    if (SwingUtilities.isRightMouseButton(mouse)) {
	        return; // Reserved for context menu
	    }

	    Prototype dat = translate(mouse.getPoint());
	    MidiEvent existing = noteAt(dat);

	    if (mouse.isShiftDown()) {
	        drag = mouse.getPoint();
	        mode = DragMode.SELECT;
	    } else if (existing == null) {
	        click = new Prototype(dat.data1, track.quantize(dat.tick));
	        drag = mouse.getPoint();
	        mode = DragMode.CREATE;
	    } else { // Clicked on an existing note
			boolean noteIsSelected = isSelectedOn(existing);
	        if (mouse.isControlDown()) {
	            // Toggle selection
	            if (noteIsSelected) {
	                removeSelectionForOn(existing);
	            } else {
	                addSelectionForOn(existing);
	            }
	        } else {
				// If not already selected, clear others and select this one
				if (!noteIsSelected) {
					select(getNoteOnAndOff(existing));
				}
				// Arm the drag for the "atomic" select-and-drag case
	            pressOnSelected = true;
				click = dat; // Store original click details for drag calculation
	        }
	    }
	    repaint();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	    if (pressOnSelected && mode == null) {
	        dragStart(e.getPoint());
	        pressOnSelected = false; // Consume the flag
	    }

	    if (mode != null) {
	        drag = e.getPoint();
	        repaint(); // Repaint to show selection rectangle or drag preview
	    }
	}

	private MidiEvent noteAt(Prototype proto) {
	    // Give priority to an exact start-tick match
	    MidiEvent exact = MidiTools.lookup(NOTE_ON, proto.data1, track.quantize(proto.tick), t);
	    if (exact != null)
	        return exact;

	    // Otherwise, check if the click is within any note's body
	    for (int i = 0; i < t.size(); i++) {
	        MidiEvent e = t.get(i);
	        if (!(e.getMessage() instanceof ShortMessage sm) || !Midi.isNoteOn(sm))
	            continue;

	        if (sm.getData1() != proto.data1) continue;

	        long noteStart = e.getTick();
	        if (noteStart > proto.tick) break; // Optimization

	        MidiEvent off = NotePairer.getOff(e, t);
	        long noteEnd = off != null ? off.getTick() : noteStart + track.getStepTicks();
	        if (proto.tick >= noteStart && proto.tick < noteEnd)
	            return e;
	    }
	    return null;
	}

	@Override public void mouseReleased(MouseEvent mouse) {
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
	                added.add(on); added.add(off);
	                editor.push(new Edit(Type.NEW, added));
	            } catch (Exception e) { RTLogger.warn(this, e); }
	            break;

	        case SELECT:
	            if (drag == null) break;
	            Prototype a = translate(drag);
	            Prototype b = translate(mouse.getPoint());
	            long leftTick = Math.min(a.tick, b.tick);
	            long rightTick = Math.max(a.tick, b.tick);
	            int lowData1 = Math.min(a.data1, b.data1);
	            int highData1 = Math.max(a.data1, b.data1);

	            List<MidiEvent> newSel = editor.getEventsInArea(leftTick, rightTick, lowData1, highData1);
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
		// Reset all temporary drag states
	    mode = null; click = null; drag = null; dragging = null; pressOnSelected = false;
	    repaint();
	}


	@Override public void dragStart(Point mouse) {
	    mode = DragMode.TRANSLATE;
		drag = mouse;
	    dragging = new ArrayList<>();
		// We need to store the full MidiNote (with on/off) for accurate drag previews
		for (MidiEvent sel : selected) {
			if (Midi.isNoteOn(sel.getMessage())) {
				MidiEvent off = NotePairer.getOff(sel, t);
				dragging.add(new MidiNote(sel, off));
			}
		}
	}

	@Override public void drag(Point mouse) {
		// This method is now effectively a no-op, as all visual updates are handled in paint()
		// based on the current mouse position. The `drag` point is updated in mouseDragged.
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
	    editor.push(e);
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

	/* --- selection helpers --- */

	private boolean isNoteInDraggingList(MidiNote note) {
		if(dragging == null || note == null) return false;
		for(MidiEvent draggedEvent : dragging){
			if(draggedEvent.getTick() == note.getTick() && draggedEvent.getMessage() instanceof ShortMessage smd
					&& note.getMessage() instanceof ShortMessage smn){
				if(smd.getData1() == smn.getData1()) return true;
			}
		}
		return false;
	}

	private boolean isSelectedOn(MidiEvent on) {
	    if (on == null || on.getMessage() == null || selected.isEmpty()) return false;
	    return selected.stream().anyMatch(sel -> sel.getTick() == on.getTick() && MidiTools.messagesMatch(sel.getMessage(), on.getMessage()));
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
}
