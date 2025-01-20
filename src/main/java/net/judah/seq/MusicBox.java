package net.judah.seq;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.JPanel;
import javax.swing.border.Border;

import lombok.Getter;
import net.judah.gui.Detached.Floating;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Updateable;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.seq.Edit.Type;
import net.judah.seq.track.MidiTrack;

public abstract class MusicBox extends JPanel implements Musician, Updateable, Floating {
	protected static final Composite transparent = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
	public static enum DragMode { CREATE, TRANSLATE, SELECT }

	@Getter protected final MidiTrack track;
	protected final Track t;
	protected final JudahClock clock;
	protected final Measure scroll;
	/** undo/redo */
	protected ArrayList<Edit> stack = new ArrayList<>();
	private int caret;
	/** absolute notes */
	@Getter protected final Notes selected = new Notes();
	protected Prototype click;
	protected Point drag = null;
	protected DragMode mode = null;
	protected final TrackList<?> tracks;

	protected Prototype recent;
	protected ArrayList<MidiPair> dragging = new ArrayList<>();
	protected int width, height;


	public MusicBox(MidiTrack midiTrack, TrackList<? extends MidiTrack> tracks) {
		this.track = midiTrack;
		this.t = track.getT();
		this.clock = track.getClock();
		scroll = new Measure(track);
		this.tracks = tracks;
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
	}

	@Override public final Prototype translate(Point p) {
		return new Prototype(toData1(p), toTick(p));
	}

	@Override public void mouseDragged(MouseEvent mouse) {
		if (mode == null)
			return;
		switch(mode) {
			case CREATE:
			case SELECT:
				repaint();
				break;
			case TRANSLATE:
				drag(mouse.getPoint());
				break;
		}
	}

	@Override public void delete() {
		push(new Edit(Type.DEL, selected));
	}

	@Override public void copy() {
		tracks.getClipboard().copy(selected, track);
	}


	@Override public void paste() { // TODO differences in track Resolution
		List<MidiPair> notes = tracks.getClipboard().paste(track);
		push(new Edit(Type.NEW, notes));
	}

	@Override public final void mouseEntered(MouseEvent e) {MainFrame.qwerty();}
	@Override public void mouseMoved(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }
	@Override public final void mouseClicked(MouseEvent e) { }
	// public void mouseReleased(MouseEvent e) { } subclass implement
	// public void mousePressed(MouseEvent e) { } subclass implement
	@Override public final void mouseWheelMoved(MouseWheelEvent wheel) {
		boolean up = wheel.getPreciseWheelRotation() < 0;

		if (selected.isEmpty()) {
			float velocity = track.getAmp() + (up ? 0.05f : -0.05f);
			if (velocity < 0)  velocity = 0;
			if (velocity > 1)  velocity = 1;
			track.setAmp(velocity);
			return;
		}
		float ratio = up ? 1.1f : 0.9f;
		ArrayList<MidiPair> replace = new ArrayList<>();
		int data2;
		ShortMessage source, create;
		for (MidiPair p : selected) {
			if (p.getOn().getMessage() instanceof ShortMessage == false)
				continue;
			source = (ShortMessage) p.getOn().getMessage();
			data2 = (int) (source.getData2() * ratio);
			if (data2 == source.getData2())
				data2 += up ? 1 : -1;
			if (data2 > 127)
				data2 = 127;
			else if (data2 < 1) // no ghosts
				data2 = 1;
			create = Midi.create(source.getCommand(), source.getChannel(), source.getData1(), data2);
			replace.add(new MidiPair(new MidiEvent(create, p.getOn().getTick()) ,p.getOff()));
		}
		editDel(selected);
		editAdd(replace);
	}

	protected void length(Edit e, boolean undo) {
		ArrayList<MidiPair> replace = new ArrayList<MidiPair>();
		long ticks = e.getDestination().tick;
		e.getNotes().forEach(p-> replace.add(new MidiPair(p.getOn(),
			new MidiEvent(p.getOff().getMessage(), p.getOn().getTick() + ticks))));
		if (undo) {
			editDel(replace);
			editAdd(e.getNotes());
		}
		else {
			editDel(e.getNotes());
			editAdd(replace);
		}
	}

	private void execute(Edit e) {
		switch (e.getType()) {
		case DEL:
			editDel(e.getNotes());
			selected.clear();
			repaint();
			break;
		case NEW:
			editAdd(e.getNotes());
			break;
		case TRANS:
			if (track.isActive())
				new Panic(track);
			transpose(e.getNotes(), e.getDestination());
			break;
		case LENGTH:
			length(e, false);
		default: // GAIN
		}
		click = null;

	}

	public void recalc() {
		resized(getWidth(), getHeight());
	}

	@Override
	public void velocity(boolean up) {
		float velocity = track.getAmp() + (up ? 0.05f : -0.05f);
		if (velocity < 0)  velocity = 0;
		if (velocity > 1)  velocity = 1;
		track.setAmp(velocity);
	}

	@Override
	public boolean undo() {
		if (stack.size() <= caret || caret < 0) {
			return false;
		}

		Edit e = stack.get(caret);
		switch (e.getType()) {
		case DEL:
			editAdd(e.getNotes());
			break;
		case NEW:
			editDel(e.getNotes());
			selected.clear();
			repaint();
			break;
		case TRANS:
			if (track.isActive())
				new Panic(track);
			decompose(e);
			break;
		case LENGTH:
			length(e, true);
		default:
			break;
		}
		caret--;
		return true;
	}

	protected void editAdd(ArrayList<MidiPair> replace) {
		selected.clear(); // add zero-based selection
		for (MidiPair p : replace) {
			selected.add(p);
			track.getT().add(p.getOn());
			if (p.getOff() != null)
				track.getT().add(p.getOff());
		}
		repaint();
	}

	protected void editDel(ArrayList<MidiPair> notes) {
		for (MidiPair p: notes) {
				MidiTools.delete(p.getOn(), track.getT());
				if (p.getOff() != null)
					MidiTools.delete(p.getOff(), track.getT());
            }
	}

	/** execute edit and add to undo stack */
	@Override
	public void push(Edit e) {
		stack.add(e);
		caret = stack.size() - 1;
		execute(e);
	}

	public Edit peek() {
		if (caret >= stack.size())
			return null;
		return stack.get(caret);
	}

	@Override
	public boolean redo() {
		if (stack.size() <= caret + 1)
			return false;
		caret++;
		execute(stack.get(caret));
		return true;
	}

	@Override
	public void selectNone() {
		selected.clear();
		repaint();
	}

	public void selectArea(long start, long end) {
		selected.clear();
		if (track.isDrums()) {
			scroll.populate();
			for (MidiPair p : scroll)
				selected.add(p);
		}
		else {
			for (int i = 0; i < t.size(); i++) {
				long tick = t.get(i).getTick();
				if (tick < start) continue;
				if (tick >= end) break;
				if (Midi.isNoteOn(t.get(i).getMessage()))
					selected.add(MidiTools.noteOff(t.get(i), t));
			}
		}
		repaint();
	}

	public void select(Notes notes) {
		selected.clear();
		selected.addAll(notes);
		repaint();
	}

	@Override
	public void selectFrame() {
		long start = track.getLeft();
		long end = start + track.getWindow();
		selectArea(start, end);
	}

	private final Rectangle shadeRect = new Rectangle();
	protected Rectangle shadeRect(int x, int y) {
		if (drag.x < x) {
			if (drag.y < y)
				shadeRect.setBounds(drag.x, drag.y, x - drag.x, y - drag.y);
			else
				shadeRect.setBounds(drag.x, y, x - drag.x, drag.y - y);
		} else {
			if (drag.y < y)
				shadeRect.setBounds(x, drag.y, drag.x - x, y - drag.y);
			else
				shadeRect.setBounds(x, y, drag.x - x, drag.y - y);
		}
		return shadeRect;
	}

	@Override public void update() {
		Border target = track.isCapture() ? Gui.RED : Gui.NO_BORDERS;
		if (getBorder() != target)
			setBorder(target);
	}

	// Color cache
	static final int GRADIENTS = 32;
	private static final Color[] VELOCITIES = new Color[GRADIENTS];
	private static final Color[] HIGHLIGHTS = new Color[GRADIENTS];
	static {
		for (int i = 0; i < GRADIENTS; i++) {
		    int alpha = (int) ((i / (float)(GRADIENTS - 1)) * 255);
            VELOCITIES[i] = new Color(0, 112, 60, alpha); // Dartmouth Green
			HIGHLIGHTS[i] = new Color(0xFF, 0xA5, 0x00, alpha); // Orange
		}
	}
    public static Color velocityColor(int data2) {
    	return VELOCITIES[data2 / 4];}
    public static Color highlightColor(int data2) {
    	return HIGHLIGHTS[data2 / 4];
    }

}

