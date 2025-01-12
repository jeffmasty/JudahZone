package net.judah.seq.beatbox;

import static net.judah.seq.MidiTools.highlightColor;
import static net.judah.seq.MidiTools.velocityColor;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import net.judah.JudahZone;
import net.judah.api.Signature;
import net.judah.drumkit.DrumType;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.midi.Midi;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiPair;
import net.judah.seq.MidiTab;
import net.judah.seq.MusicBox;
import net.judah.seq.Prototype;

public class BeatBox extends MusicBox {

	public static final int X_OFF = 5;
	private static final int RADIUS = 24;
    private static final int PADS = DrumType.values().length;

	private final int rowHeight;
	private final float totalWidth;
	private float unit;
	private Prototype recent;
	private ArrayList<MidiPair> dragging = new ArrayList<>();

    public BeatBox(final Rectangle r, BeatsSection view, MidiTab tab) {
    	super(view, r, tab);
		totalWidth = r.width;
    	rowHeight = (int)Math.ceil((r.height) / PADS);
		setOpaque(true);
    	setBackground(Pastels.MY_GRAY);
        setLayout(null);
        timeSig(JudahZone.getClock().getTimeSig());
    }

    @Override public void timeSig(Signature sig) {
		unit = totalWidth / (2f * sig.steps);
		repaint();
	}

	@Override public long toTick(Point p) {
		return track.quantize((long) (p.x / totalWidth * track.getWindow() + track.getLeft()));
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
	@Override public int toData1(Point p) {
		return toDrumType(p).getData1();
	}

    @Override public void paint(Graphics g) {
    	super.paint(g);
    	// background ovals
        int stepz = clock.getSteps();
        for (int x = 0; x < 2 * stepz ; x++) {
            for (int y = 0; y < PADS; y++) {
            	drumpad(g, (int) (x * unit), y, x % clock.getSubdivision() == 0 ? Pastels.BLUE :Color.WHITE);
            	if (x % stepz == 0)
            		mini(g, (int) (x * unit), y);
            }
        }

        long offset = track.getLeft();
        ShortMessage on;
        int x, y;
        float window = track.getWindow();
        for (MidiPair p : scroll.populate()) {
        	on = (ShortMessage)p.getOn().getMessage();
			y = DrumType.index(on.getData1());
			x = (int) ( (  (p.getOn().getTick()-offset) / window) * totalWidth);
			if (y >=0 && x >= 0) {
				if (selected.contains(p))
					highlight(g, x, y, on.getData2());
				else
					drumpad(g, x, y, on.getData2());
			}
        }
        if (mode == DragMode.SELECT) {
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
    }

    private void drumpad(Graphics g, int x, int y, Color c) {
    	g.setColor(c);
    	g.fillOval(x + X_OFF, y * rowHeight + 2, RADIUS, RADIUS);
    }

    private void mini(Graphics g, int x, int y) {
    	g.setColor(Color.WHITE);
    	g.fillOval(x + X_OFF + RADIUS / 3, y * rowHeight + 2 + RADIUS / 3, RADIUS / 3, RADIUS / 3);
    }

    private void drumpad(Graphics g, int x, int y, int data2) {
    	drumpad(g, x, y, velocityColor(data2));
    }

    private void highlight(Graphics g, int x, int y, int data2) {
    	drumpad(g, x, y, highlightColor(data2));
    }

    @Override public void mousePressed(MouseEvent mouse) {
    	((BeatsTab)tab).setCurrent(view.getTrack());
    	click = translate(mouse.getPoint());
		MidiEvent existing = track.get(NOTE_ON, click.data1, click.tick);
		if (mouse.isShiftDown()) { // drag-n-select;
			drag = mouse.getPoint();
			mode = DragMode.SELECT;
		}
		else if (existing == null) {
			drag = mouse.getPoint();
			mode = DragMode.CREATE;
		}
		else {
			MidiPair pair = new MidiPair(existing, null);
			if (mouse.isControlDown()) {
				if (selected.contains(pair))
					selected.remove(pair);
				else
					selected.add(pair);
			}
			else {
				if (selected.contains(pair)) {
					dragStart(mouse.getPoint()); // already selected, start drag-n-drop
				}
				else { // simple select click
					selected.clear();
					selected.add(pair);
				}
			}
		}
		repaint();
    }

	@Override public void mouseReleased(MouseEvent mouse) {
		if (mode != null) {
			switch(mode) {
			case CREATE:
				MidiEvent create = new MidiEvent(Midi.create(NOTE_ON, track.getCh(), click.data1,
						(int) (track.getAmp() * 127f)), click.tick);
				push(new Edit(Type.NEW, new MidiPair(create, null)));
				break;
			case SELECT:
				Prototype a = translate(drag);
				Prototype b = translate(mouse.getPoint());
				drag = null;
				selectArea(a.tick, b.tick, a.data1, b.data1);
				break;
			case TRANSLATE:
				drop(mouse.getPoint());
				break;
			}
			mode = null;
			repaint();
			click = null;
		}
        MainFrame.qwerty();
	}

	@Override
	public void selectArea(long start, long end, int low, int high) {
		selected.clear();
		for (int i = 0; i < t.size(); i++) {
			long tick = t.get(i).getTick();
			if (tick < start) continue;
			if (tick >= end) break;
			if (Midi.isNoteOn(t.get(i).getMessage())) {
				MidiEvent evt = t.get(i);
				int data1 = ((ShortMessage)evt.getMessage()).getData1();
				if (data1 >= low && data1 <= high)
					selected.add(new MidiPair(evt, null));
			}
		}
		repaint();
	}

	//////// Drag and Drop /////////
	@Override public void dragStart(Point mouse) {
		mode = DragMode.TRANSLATE;
		dragging.clear();
		selected.forEach(p -> dragging.add(new MidiPair(p)));
		click = translate(mouse);
		recent = new Prototype(toData1(mouse), click.tick);
	}



	@Override
	public void drop(Point mouse) {
		// delete selected, create undo from start/init
		editDel(selected);
		Edit e = new Edit(Type.TRANS, dragging);
		long now = track.quantize(toTick(mouse));
		Prototype destination = new Prototype(toData1(mouse),
				((now - click.tick) % track.getWindow()) / track.getStepTicks());
		e.setDestination(destination, click);
		push(e);
	}

	@Override
	public void drag(Point mouse) {

		Prototype now = new Prototype(toData1(mouse), track.quantize(toTick(mouse)));
		if (now.equals(recent)) // hovering
			return;
		// note or step changed, move from most recent drag spot
		long tick = ((now.tick - recent.tick) % track.getWindow()) / track.getStepTicks();
		recent = now;
		transpose(selected, new Prototype(now.data1, tick));

	}

	@Override
	public void transpose(ArrayList<MidiPair> notes, Prototype destination) {
		editDel(notes);

		ArrayList<MidiPair> replace = new ArrayList<>();
		int delta = DrumType.index(destination.data1) - DrumType.index(click.data1);
		long window = track.getWindow();
		long start = track.getCurrent() * track.getBarTicks();
		for (MidiPair note : notes)
			replace.add(compute(note, delta, destination.tick, start, window));
		editAdd(replace);
	}

	@Override
	public void decompose(Edit e) {
		ArrayList<MidiPair> notes = e.getNotes();
		Prototype destination = e.getDestination();
		ArrayList<MidiPair> delete = new ArrayList<>();
		long window = track.getWindow();
		long start = track.getCurrent() * track.getBarTicks();
		int delta = DrumType.index(destination.data1) - DrumType.index(
				((ShortMessage)notes.get(e.getIdx()).getOn().getMessage()).getData1());
		for (MidiPair note : notes)
			delete.add(compute(note, delta, destination.tick, start, window));
		editDel(delete);
		editAdd(notes);
		click = null;
	}

	/**@param in source note (off is null for drums)
	 * @param destination x = +/-ticks,   y = +/-data1
	 * @return new midi */
	public MidiPair compute(MidiPair in, int delta, long protoTick, long start, long window) {
		ShortMessage source = (ShortMessage)in.getOn().getMessage();
		long tick = in.getOn().getTick() + protoTick * track.getStepTicks();
		if (tick < start) tick += window;
		if (tick >= start + window) tick -= window;
		int data1 =  DrumType.translate(source.getData1(), delta);
		return new MidiPair(new MidiEvent(Midi.create(source.getCommand(), source.getChannel(), data1, source.getData2()), tick), null);
	}

}
