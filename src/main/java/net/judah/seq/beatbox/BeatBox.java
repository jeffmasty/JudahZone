package net.judah.seq.beatbox;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.SwingUtilities;

import judahzone.api.Midi;
import judahzone.api.Signature;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import net.judah.drumkit.DrumType;
import net.judah.gui.TabZone;
import net.judah.seq.automation.CCPopup;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.Edit;
import net.judah.seq.track.MidiNote;
import net.judah.seq.track.MidiTools;
import net.judah.seq.track.MusicBox;
import net.judah.seq.track.Prototype;
import net.judah.seq.track.Edit.Type;
import net.judah.seq.track.Editor.Selection;

public class BeatBox extends MusicBox implements Pastels {

	public static final int X_OFF = 5;
	private static final int PADS = DrumType.values().length;

	private int rowHeight;
	private float unitX, unitY;
	private int minusX, minusY;
	private final DrumZone tab;
	private final CCPopup cc;
	private DrumType ccType = DrumType.Bongo;
	private boolean pressOnSelected = false;

	public BeatBox(DrumTrack t, DrumZone tab) {
		super(t);
		this.tab = tab;
		cc = new CCPopup(t, this, true);
		setLayout(null);
		repaint();
	}

	@Override
	public void selectionChanged(Selection selection) {
	    // Don't call super - we handle everything here
	    if (selection == null || selection.originId() == this) {
	        return; // Ignore our own events
	    }

	    // Update internal selection WITHOUT re-publishing
	    selected.clear();
	    if (selection.events() != null) {
	        for (MidiEvent e : selection.events()) {
	            selected.add(new MidiNote(e));
	        }
	    }
	    repaint();
	}


	@Override
	public long toTick(Point p) {
		return track.quantize((long) (p.x / (float) width * track.getWindow() + track.getLeft()));
	}

	public int toDrumTypeIdx(Point p) {
		int result = p.y / rowHeight;
		if (result < 0)
			return 0;
		if (result >= DrumType.values().length)
			return DrumType.values().length - 1;
		return result;
	}

	public DrumType toDrumType(Point p) {
		return DrumType.values()[toDrumTypeIdx(p)];
	}

	@Override
	public int toData1(Point p) {
		return toDrumType(p).getData1();
	}

	@Override
	public void paint(Graphics g) {
		Color bg, shade;
		if (tab == null || tab.getCurrent() == track) {
			bg = EGGSHELL;
			shade = SHADE;
		} else {
			bg = SHADE;
			shade = EGGSHELL;
		}
		g.setColor(bg);
		g.fillRect(0, 0, width, height);

		int stepz = clock.getSteps();
		int range = 2 * stepz;

		// shade beats and downbeats
		for (int beat = 0; beat < 2 * clock.getTimeSig().beats; beat++) {
			int x1 = (int) (beat * 4 * unitX);
			g.setColor((beat % clock.getMeasure() == 0) ? DOWNBEAT : shade);
			g.fillRect(x1, 0, (int) unitX, height);
		}

		// draw grid
		g.setColor(GRID);
		for (int x = 0; x < range; x++)
			g.drawLine((int) (x * unitX), 0, (int) (x * unitX), height);
		for (int y = 0; y <= PADS; y++)
			g.drawLine(0, (int) (y * unitY), width, (int) (y * unitY));

		// draw notes
		long offset = track.getLeft();
		float scale = width / (float) track.getWindow();
		ShortMessage on;
		int x, y;
		for (MidiNote p : scroll.populate()) {
			on = (ShortMessage) p.getMessage();
			y = DrumType.index(on.getData1());
			x = (int) ((p.getTick() - offset) * scale);
			if (selected.contains(p)) {
				if (dragging == null)
					highlight(g, x, y, on.getData2());
			} else
				drumpad(g, x, y, on.getData2());
		}
		if (dragging != null) {
			for (MidiEvent p : dragging) {
				on = (ShortMessage) p.getMessage();
				y = DrumType.index(on.getData1());
				x = (int) ((p.getTick() - offset) * scale);
				highlight(g, x, y, 100);
			}
		}

		// draw CCs/Progs/Pitch
		int[] ccSteps = cc.populate(track.getLeft(), track.getLeft() + track.getWindow());

		for (int step = 0; step < ccSteps.length; step++) {
			if (cc.getProg(step) != null)
				ccPad(g, step, 1, Pastels.PROGCHANGE);
			if (ccSteps[step] > 0)
				ccPad(g, step, ccSteps[step], Pastels.CC);
		}

		if (mode == DragMode.SELECT) {
			// highlight drag region
			Graphics2D g2d = (Graphics2D) g;
			Composite original = g2d.getComposite();
			g2d.setComposite(transparent);
			g2d.setPaint(mode == DragMode.SELECT ? SHADE : SELECTED);
			Point origin = getLocationOnScreen();
			Point mouse = MouseInfo.getPointerInfo().getLocation();
			g2d.fill(shadeRect(mouse.x - origin.x, mouse.y - origin.y));
			g2d.setComposite(original);
		}

	}

	private void ccPad(Graphics g, int step, int count, Color c) {
		g.setColor(c);
		int x = (int) (step * unitX);
		int y = ccType.ordinal() * rowHeight;
		g.fillRect(x, y, (int) unitX, minusY);
		if (count > 1) {
			g.setColor(Color.BLACK);
			g.drawString("" + count, x + (int) (unitX / 2f) - 4, y + (int) (unitY / 2f) + 1);
		}
	}

	private void drumpad(Graphics g, int x, int y, Color c) {
		g.setColor(c);
		g.fillRect(x + 2, y * rowHeight + 1, minusX, minusY);
	}

	private void drumpad(Graphics g, int x, int y, int data2) {
		drumpad(g, x, y, velocityColor(data2));
	}

	private void highlight(Graphics g, int x, int y, int data2) {
		drumpad(g, x, y, highlightColor(data2));
	}

	@Override
	public void timeSig(Signature sig) {
		unitX = width / (2f * sig.steps);
		minusX = (int) unitX - 3;
		repaint();
	}

	@Override
	public void resized(int w, int h) {
		width = w;
		height = h;
		unitY = height / (float) PADS;
		minusY = (int) unitY - 5;
		rowHeight = (int) Math.ceil((height) / (float) PADS);
		timeSig(clock.getTimeSig());
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		resized(d.width, d.height);
		Gui.resize(this, d);
	}

	@Override
	public void mousePressed(MouseEvent mouse) {
	    tab.setCurrent(track); // drums only
	    click = translate(mouse.getPoint());
	    pressOnSelected = false; // reset

	    if (SwingUtilities.isRightMouseButton(mouse)) {
	        int step = (int) (click.tick % track.getWindow() / track.getStepTicks());
	        cc.popup(mouse, step);
	        return;
	    }

	    MidiEvent existing = MidiTools.lookup(NOTE_ON, click.data1, click.tick, t);
	    if (mouse.isShiftDown()) { // drag-n-select;
	        drag = mouse.getPoint();
	        mode = DragMode.SELECT;
	        pressOnSelected = false;
	    } else if (existing == null) {
	        // prepare for create
	        drag = mouse.getPoint();
	        mode = DragMode.CREATE;
	        pressOnSelected = false;
	    } else {
	        MidiNote pair = new MidiNote(existing);
	        if (mouse.isControlDown()) {
	            // toggle selection but do not arm drag
	            if (selected.contains(pair))
	                selected.remove(pair);
	            else
	                selected.add(pair);
	            pressOnSelected = false;
	        } else {
	            // Select the clicked note. Arm a pending-drag so movement starts a translate.
	            if (!selected.contains(pair)) {
	                selected.clear();
	                selected.add(pair);
	            }
	            pressOnSelected = true;
	            // Do NOT call dragStart here; actual drag begins on mouse movement (drag).
	        }
	    }
	    repaint();
	}

	@Override
	public void mouseReleased(MouseEvent mouse) {
	    if (mode != null) {
	        switch (mode) {
	        case CREATE:
	            MidiEvent create = new MidiEvent(
	                    Midi.create(NOTE_ON, track.getCh(), click.data1, (int) (track.getAmp() * 127f)), click.tick);

	            // Create a list for the new event
	            ArrayList<MidiEvent> added = new ArrayList<>();
	            added.add(create);

	            // Publish the selection to the Editor with this BeatBox as the origin
	            select(added);

	            // Push the actual edit using the same list
	            track.getEditor().push(new Edit(Type.NEW, added));
	            break;

	        case SELECT:
	            Prototype a = translate(drag);
	            Prototype b = translate(mouse.getPoint());
	            drag = null;
	            // Query the editor for events in the area and publish that as the new selection
	            List<MidiEvent> sel = editor.selectArea(a.tick, b.tick, a.data1, b.data1);
	            select(sel);
	            break;

	        case TRANSLATE:
	            drop(mouse.getPoint());
	            break;
	        }
	        mode = null;
	        repaint();
	        click = null;
	    }
	    // Always clear the pending-drag flag on release
	    pressOnSelected = false;
	    TabZone.instance.requestFocusInWindow();
	}


	@Override
	public void mouseDragged(MouseEvent e) {
	    // If a press on a selected note is pending, start the drag now.
	    if (pressOnSelected && mode == null) {
	        dragStart(e.getPoint());
	        pressOnSelected = false; // Consume the flag
	    }
	    // Continue with drag processing if in a drag mode
	    if (mode == DragMode.TRANSLATE) {
	        drag(e.getPoint());
	    } else if (mode == DragMode.SELECT) {
	        repaint(); // Repaint to show selection rectangle
	    }
	}

	//////// Drag and Drop /////////
	@Override
	public void dragStart(Point mouse) {
		mode = DragMode.TRANSLATE;
		dragging = new ArrayList<>(selected);
		click = translate(mouse);
		recent = new Prototype(toData1(mouse), click.tick);
	}

	@Override
	public void drop(Point mouse) {
		// delete selected, create undo from start/init
		Edit e = new Edit(Type.TRANS, selected);
		long now = toTick(mouse);
		Prototype destination = new Prototype(toData1(mouse),
				((now - click.tick) % track.getWindow()) / track.getStepTicks());
		e.setDestination(destination, click);
		editor.push(e);
		select(dragging);
		dragging = null;
		click = null;
	}

	@Override
	public void drag(Point mouse) {
		Prototype now = new Prototype(toData1(mouse), toTick(mouse));
		if (now.equals(recent)) // hovering
			return;
		// note or step changed, move from most recent drag spot
		long tick = ((now.tick - recent.tick) % track.getWindow()) / track.getStepTicks();
		Prototype dest = new Prototype(now.data1, tick);
		transpose(dest);
		recent = now;
	}

	private void transpose(Prototype destination) {
		int delta = DrumType.index(destination.data1)
				- DrumType.index(((ShortMessage) dragging.getFirst().getMessage()).getData1());
		long start = track.getCurrent() * track.getBarTicks();
		for (int i = 0; i < dragging.size(); i++) {
			MidiEvent note = dragging.get(i);
			dragging.set(i, new MidiNote(editor.compute(note, delta, destination.tick, start, track.getWindow())));
		}
		repaint();
	}

	/**
	 * @param in          source note (off is null for drums)
	 * @param destination x = +/-ticks, y = +/-data1
	 * @return new midi
	 */
	public MidiNote compute(MidiNote in, int delta, long protoTick, long start, long window) {
		ShortMessage source = (ShortMessage) in.getMessage();
		long tick = in.getTick() + protoTick * track.getStepTicks();
		if (tick < start)
			tick += window;
		if (tick >= start + window)
			tick -= window;
		int data1 = DrumType.translate(source.getData1(), delta);
		return new MidiNote(
				new MidiEvent(Midi.create(source.getCommand(), source.getChannel(), data1, source.getData2()), tick));
	}

	/** remove any note-offs */
	public void clean() {
		for (int i = t.size() - 1; i > -1; i--)
			if (Midi.isNoteOff(t.get(i).getMessage()))
				t.remove(t.get(i));
	}

	/** remove any notes that don't fit inside DrumTypes */
	public void condense() {
		clean();
		ShortMessage m;
		for (int i = t.size() - 1; i > -1; i--) {
			if (Midi.isNoteOn(t.get(i).getMessage())) {
				m = (ShortMessage) t.get(i).getMessage();
				if (DrumType.index(m.getData1()) < 0)
					t.remove(t.get(i));
			}
		}
	}

	public void setCCType(DrumType t) {
		ccType = t;
		repaint();
	}

}
