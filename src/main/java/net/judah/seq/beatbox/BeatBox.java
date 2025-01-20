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

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import net.judah.api.Signature;
import net.judah.drumkit.DrumType;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.midi.Midi;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiPair;
import net.judah.seq.MusicBox;
import net.judah.seq.Prototype;
import net.judah.seq.track.DrumTrack;

public class BeatBox extends MusicBox implements Pastels{

	public static final int X_OFF = 5;
    private static final int PADS = DrumType.values().length;

	private int rowHeight;
	private float unitX, unitY;
	private int minusX, minusY;
	private final DrumZone tab;

    public BeatBox(DrumTrack t, DrumZone tab) {
    	super(t, tab.getTracks());
    	this.tab = tab;
        setLayout(null);
        update();
    }

	@Override public long toTick(Point p) {
		return track.quantize((long) (p.x / (float)width * track.getWindow() + track.getLeft()));
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
    	// super.paint(g);


    	Color bg, shade;
    	if (tracks.getCurrent() == track) {
    		bg = EGGSHELL;
    		shade = SHADE;
    	}
    	else {
    		bg = SHADE;
    		shade = EGGSHELL;
    	}
    	g.setColor(bg);
    	g.fillRect(0, 0, width, height);

        int stepz = clock.getSteps();
		int range = 2 * stepz;

    	// shade beats and downbeats
		for (int beat = 0; beat < 2 * clock.getTimeSig().beats; beat++) {
			int x1 = (int)(beat * 4 * unitX);
			g.setColor((beat % clock.getMeasure() == 0) ? DOWNBEAT : shade);
			g.fillRect(x1, 0, (int)unitX, height);
		}

    	// draw grid
        g.setColor(GRID);
        for (int x = 0; x < range; x++)
        	g.drawLine((int)(x * unitX), 0, (int)(x * unitX), height);
        for (int y = 0; y <= PADS; y++)
        	g.drawLine(0, (int)(y * unitY), width, (int)(y * unitY));

        // draw notes
        long offset = track.getLeft();
        ShortMessage on;
        int x, y;
        float window = track.getWindow();
        for (MidiPair p : scroll.populate()) {
        	on = (ShortMessage)p.getOn().getMessage();
			y = DrumType.index(on.getData1());
			x = (int) ( (  (p.getOn().getTick()-offset) / window) * width);
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
			g2d.setPaint(mode == DragMode.SELECT ? SHADE : SELECTED);
			Point origin = getLocationOnScreen();
			Point mouse = MouseInfo.getPointerInfo().getLocation();
			g2d.fill(shadeRect(mouse.x - origin.x, mouse.y - origin.y));
			g2d.setComposite(original);
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

    @Override public void timeSig(Signature sig) {
		unitX = width / (2f * sig.steps);
		minusX = (int)unitX - 3;
		repaint();
	}

    @Override
    public void resized(int w, int h) {
    	width = w;
    	height = h;
		unitY = height / (float)PADS;
		minusY = (int)unitY - 5;
    	rowHeight = (int)Math.ceil((height) / (float)PADS);
    	timeSig(clock.getTimeSig());
    }

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		resized(d.width, d.height);
		Gui.resize(this, d);
	}

    @Override public void mousePressed(MouseEvent mouse) {
		tab.setCurrent(track); // drums only
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

	@Override public void update() {
		super.update();
		repaint();
	}

}


/*MidiView
@Override
public void setSize(Dimension d) {
	grid.setSize(d);
}

public static int toData1(Point p) {
	return p.x / BEAT_WIDTH + DATA1_OFFSET;
}
public static int toX(int data1) {
	return (data1 - DATA1_OFFSET) * BEAT_WIDTH;
}

public static int toY(long tick, long measure, int height) {
	return (int) (ratioY(tick, measure) * height);
}

public static float ratioY(long tick, long measure) {
	return tick / (float)measure;
}

public static long quantize(long tick, int resolution, Gate gate) { // TODO only does 1/16th quantization for now
	long unit = (long) (0.25f * resolution);
	float units = tick / unit;
	return (long)units * unit;
}
*/