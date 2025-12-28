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

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.BoxLayout;

import net.judah.api.Key;
import net.judah.api.Midi;
import net.judah.api.Signature;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiNote;
import net.judah.seq.MidiTools;
import net.judah.seq.MusicBox;
import net.judah.seq.Notes;
import net.judah.seq.Prototype;
import net.judah.seq.track.MidiTrack;
import net.judah.util.RTLogger;

/** display midi music in piano grid */
public class Piano extends MusicBox {

	private final PianoKeys piano;
	private final PianoSteps steps;
	private final PianoView zoom;

	private float ratio;
	private float unit;

	public Piano(PianoView view, PianoSteps currentBeat, PianoKeys roll) {
		super(view.track);
		this.zoom = view;
		this.steps = currentBeat;
		this.piano = roll;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}

	@Override public long toTick(Point p) {
		return (long) (p.y / (float)(height) * track.getWindow() + track.getLeft());
	}

	@Override public int toData1(Point p) {
		return (int) (p.x / zoom.scaledWidth) + zoom.tonic;
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

		int yheight, data1;
		for (MidiNote p : scroll.populate()) {

			if (p.getMessage() instanceof ShortMessage s) {
				data1 = s.getData1();
				if (data1 < tonic || data1 > top)
					continue;

				if (selected.contains(p.getTick(), data1))
					g.setColor(dragging == null ? highlightColor(s.getData2()) : Pastels.YELLOW);

				else
					g.setColor(velocityColor(s.getData2()));
				x = (int) (noteWidth * (data1 - tonic));
				y = (int) ((p.getTick() - track.getLeft()) * ratio);
				yheight = (int) ((p.getOff().getTick() - p.getTick()) * ratio);
				g.fill3DRect(x, y, keyWidth, yheight, true);
			}
		}
        if (dragging != null) {
    		g.setColor(highlightColor(127));
        	for (MidiNote p : dragging) {
				x = (int) (noteWidth * (((ShortMessage)p.getMessage()).getData1() - tonic));
				y = (int) ((p.getTick() - track.getLeft()) * ratio);
				yheight = (int) ((p.getOff().getTick() - p.getTick()) * ratio);
				g.fill3DRect(x, y, keyWidth, yheight, true);
        	}
        }

		g.setColor(Pastels.FADED);
		g.drawRect(0, 0, width-1, height - 1); // border

		if (mode == null)
			return;

		if (mode == DragMode.SELECT || mode == DragMode.CREATE) {
			// highlight drag region
			Graphics2D g2d = (Graphics2D)g;
			Composite original = g2d.getComposite();
			g2d.setComposite(transparent);
			g2d.setPaint(mode == DragMode.SELECT ? Pastels.BLUE : Pastels.ORANGE);
			Point origin = getLocationOnScreen();
			Point mouse = MouseInfo.getPointerInfo().getLocation();
			g2d.fill(shadeRect(mouse.x - origin.x, mouse.y - origin.y));
			g2d.setComposite(original);
		}
		// else if (mode == DragMode.TRANSLATE) // shown as selected
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
		Prototype dat = translate(mouse.getPoint());
		MidiNote existing = MidiTools.lookup(dat.tick, dat.data1, t);

		if (mouse.isShiftDown()) { // drag-n-select;
			drag = mouse.getPoint();
			mode = DragMode.SELECT;
		}
		else if (existing == null) {
			click = new Prototype(dat.data1, track.quantize(dat.tick));
			drag = mouse.getPoint();
			mode = DragMode.CREATE;
		}
		else if (mouse.isControlDown()) {
			if (selected.contains(existing))
				selected.remove(existing);
			else
				selected.add(existing);
		}
		else {
			if (selected.contains(existing)) {
				dragStart(mouse.getPoint()); // already selected, start drag-n-drop
			}
			else { // simple select click
				selected.clear();
				selected.add(existing);
			}
		}
		repaint();
	}

	@Override public void mouseReleased(MouseEvent mouse) {
		if (mode != null) {
			switch(mode) {
			case CREATE: try {
				long left = click.tick;
				long right = toTick(mouse.getPoint());
				if (left > right) {
					left = right;
					right = click.tick;
				}
				int data1 = toData1(mouse.getPoint());
				MidiEvent on = Midi.createEvent(left, NOTE_ON,
						track.getCh(), data1, (int) (track.getAmp() * 127f));
				MidiEvent off = Midi.createEvent(track.quantizePlus(right), NOTE_OFF, track.getCh(), data1, 1);
				track.getEditor().push(new Edit(Type.NEW, on, off));
				} catch (Exception e) {
					RTLogger.warn(this, e); }
				break;
			case SELECT:
				if (drag == null) break;
				Prototype a = translate(drag);
				Prototype b = translate(mouse.getPoint());
				long left = a.tick;
				long right = b.tick;
				if (left > right) {
					left = b.tick;
					right = a.tick;
				}
				int high = a.data1;
				int low = b.data1;
				if (low > high) {
					high = b.data1;
					low = a.data1;
				}
				drag = null;
				selectArea(left, right, low, high);
				break;
			case TRANSLATE:
				drop(mouse.getPoint());
				break;
			}
			mode = null;
			repaint();
		}

	}

	@Override public Notes selectArea(long start, long end, int low, int high) {
		selected.clear();
		for (int i = 0; i < t.size(); i++) {
			long tick = t.get(i).getTick();
			if (tick < start) continue;
			if (tick >= end) break;
			if (Midi.isNoteOn(t.get(i).getMessage())) {
				MidiEvent evt = t.get(i);
				int data1 = ((ShortMessage)evt.getMessage()).getData1();
				if (data1 >= low && data1 <= high)
					selected.add(MidiTools.noteOff(evt, t));
			}
		}
		repaint();
		return selected;
	}

	//////// Drag and Drop /////////
	@Override public void dragStart(Point mouse) {
		mode = DragMode.TRANSLATE;
		dragging = new ArrayList<>(selected);
		click = recent = new Prototype(toData1(mouse), track.quantize(toTick(mouse)));
		// if (track.isActive())  new Panic(track); sounding the old notes
	}

	@Override public void drag(Point mouse) {
		piano.highlight(toData1(mouse));
		steps.highlight(mouse);
		Prototype now = new Prototype(toData1(mouse), track.quantize(toTick(mouse)));
		if (now.equals(recent)) // hovering
			return;
		// note or step changed, move from most recent drag spot
		int data1 = now.data1 - recent.data1;
		long tick = ((now.tick - recent.tick) % track.getWindow()) / track.getStepTicks();
		Prototype destination = new Prototype(data1, tick);
		recent = now;
		transpose(destination);
		repaint();
	}

	@Override public void drop(Point mouse) {
		Edit e = new Edit(Type.TRANS, selected);
		Prototype now = new Prototype(toData1(mouse), track.quantize(toTick(mouse)));
		Prototype destination = new Prototype(now.data1 - click.data1,
				((now.tick - click.tick) % track.getWindow()) / track.getStepTicks());
		e.setDestination(destination);
		editor.push(e);
		select(dragging);
		dragging = null;
		click = null;
	}

	private void transpose(Prototype destination) {
		for (int i = 0; i < dragging.size(); i++) {
			MidiNote note = dragging.get(i);
			MidiNote trans = compute(new MidiNote(new MidiEvent(note.getMessage(), note.getTick()), note.getOff()), destination, track);
			dragging.set(i, new MidiNote(trans, trans.getOff()));
		}
	}

	/**@param in source note (off is null for drums)
	 * @param destination x = +/-ticks,   y = +/-data1
	 * @return new midi */
	public static MidiNote compute(MidiNote in, Prototype destination, MidiTrack t) {
		if (in.getMessage() instanceof ShortMessage == false)
			return in;
		MidiEvent on = transpose((ShortMessage)in.getMessage(), in.getTick(), destination, t);
		MidiEvent off = null;
		if (in.getOff() != null)
			off = transpose((ShortMessage)in.getOff().getMessage(), in.getOff().getTick(), destination, t);
		return new MidiNote(on, off);
	}

	static MidiEvent transpose(ShortMessage source, long sourceTick, Prototype destination, MidiTrack t) {
		long window = t.getWindow();
		long start = t.getCurrent() * t.getBarTicks();
		long tick = sourceTick + (destination.tick * t.getStepTicks());

		// TODO wonky, need to split?
		if (tick < start) tick += window;
		if (tick >= start + window) tick -= window;

		int data1 = source.getData1() + destination.data1;
		if (data1 < 0) data1 += Key.OCTAVE;
		if (data1 > 127) data1 -= Key.OCTAVE;
		return new MidiEvent(Midi.create(source.getCommand(), source.getChannel(), data1, source.getData2()), tick);
	}

}

