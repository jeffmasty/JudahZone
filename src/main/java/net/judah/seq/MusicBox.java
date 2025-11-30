package net.judah.seq;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.JPanel;
import javax.swing.border.Border;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Detached.Floating;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.TabZone;
import net.judah.gui.Updateable;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.seq.Edit.Type;
import net.judah.seq.track.NoteTrack;
import net.judah.util.RTLogger;

public abstract class MusicBox extends JPanel implements Musician, Updateable, Floating {

	public static enum DragMode { CREATE, TRANSLATE, SELECT }
	protected static final Composite transparent = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

	@Getter protected final NoteTrack track;
	protected final Track t;
	protected final JudahClock clock;
	protected final Measure scroll;
	/** absolute notes */
	@Getter protected final Notes selected = new Notes();
	protected final ArrayList<MidiPair> dragging = new ArrayList<>();
	/** undo/redo */
	protected ArrayList<Edit> stack = new ArrayList<>();

	private int caret;
	protected Prototype click;
	protected Point drag = null;
	protected DragMode mode = null;
	protected Prototype recent;
	protected int width, height;
	protected final Clipboard clipboard;

	public MusicBox(NoteTrack midiTrack) {
		this.track = midiTrack;
		this.t = track.getT();
		this.clock = track.getClock();
		this.clipboard = JudahZone.getSeq().getClipboard();
		scroll = new Measure(track);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
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

	@Override public abstract void mouseReleased(MouseEvent e); // subclass implement
	@Override public abstract void mousePressed(MouseEvent e); // subclass implement;
	@Override public final void mouseEntered(MouseEvent e) {TabZone.instance.requestFocusInWindow();}
	@Override public void mouseMoved(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }
	@Override public final void mouseClicked(MouseEvent e) { }

	@Override public final void mouseWheelMoved(MouseWheelEvent wheel) {
		boolean up = wheel.getPreciseWheelRotation() < 0;
		if (wheel.isControlDown())
			shiftVelocity(up);
		else if (wheel.isShiftDown())
			translate(up); // shift selected note timing

		else if (wheel.isAltDown()) {
			transpose(up); // shift selected note pitch
		}
		else
			track.next(!up);
	}

	@Override public void velocity(boolean up) {
		float velocity = track.getAmp() + (up ? 0.05f : -0.05f);
		if (velocity < 0)  velocity = 0;
		if (velocity > 1)  velocity = 1;
		track.setAmp(velocity);
	}

	private void shiftVelocity(boolean up) {
		if (selected.isEmpty()) {
			velocity(up);
			return;
		}
		// change selected notes' velocity
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

	private void transpose(boolean up) {
		if (selected.isEmpty()) return;
		MidiPair a = selected.getFirst();
		int src = ((ShortMessage)a.getOn().getMessage()).getData1();
		int dest = src + (up ?  1 : -1);
		if (dest < 0 || dest > 127)
			return;
		Edit e = new Edit(Type.TRANS, selected);
		e.setDestination(new Prototype(dest, a.getOn().getTick()));
		push(e);
	}

	private void translate(boolean up) {
		if (selected.isEmpty()) return;
		MidiPair a = selected.getFirst();
		long src = a.getOn().getTick();
		long plus = track.quantizePlus(src);
		long dest = up ? plus : src - (plus - src);

		if (dest > track.getLeft() + track.getWindow())
			dest -= track.getWindow();
		if (dest < track.getLeft())
			dest += track.getWindow();

		Edit e = new Edit(Type.TRANS, selected);
		e.setDestination(new Prototype(((ShortMessage)a.getOn().getMessage()).getData1(), dest));
		push(e);
	}

	@Override public final Prototype translate(Point p) {
		return new Prototype(toData1(p), toTick(p));
	}

	@Override public void delete() {
		push(new Edit(Type.DEL, selected));
	}

	@Override public void copy() {
		clipboard.copy(selected, track);
	}

	@Override public void paste() {
		List<MidiPair> notes = clipboard.paste(track);
		push(new Edit(Type.NEW, notes));
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
		MainFrame.update(this);
	}

	private void execute(Edit e) {
		switch (e.getType()) {
		case DEL:
			editDel(e.getNotes());
			selectNone();
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
			break;
		case REMAP:
			remap(e, true);
			break;
		case TRIM:
			trim(e, true);
			break;
		case INS:
			insert(e, true);
			break;
		case MOD:
			mod(e, true);
			break;
		}
		click = null;
		RTLogger.debug("exe: " + e.getType() + " to " + e.getDestination() + ": " + Arrays.toString(e.getNotes().toArray()));
	}

	@Override public void undo() {
		if (stack.size() <= caret || caret < 0) {
			RTLogger.debug("undo empty");
			return;
		}

		Edit e = stack.get(caret);
		switch (e.getType()) {
		case DEL:
			editAdd(e.getNotes());
			break;
		case NEW:
			editDel(e.getNotes());
			selectNone();
			break;
		case TRANS:
			if (track.isActive())
				new Panic(track);
			decompose(e);
			break;
		case LENGTH:
			length(e, true);
			break;
		case REMAP:
			remap(e, false);
			break;
		case TRIM:
			trim(e, false);
			break;
		case INS:
			insert(e, false);
			break;
		case MOD:
			mod(e, false);
			break;
		}

		caret--;
		RTLogger.debug("undo: " + e.getType());
	}


	protected abstract void remap(Edit e, boolean exe);

	// add blank tape by moving existing notes
	protected void insert(Edit e, boolean exe) {
		long start = e.getOrigin().tick;
		long end = e.getDestination().tick;
		long diff = end - start;
		if (exe)
			MidiTools.addTape(t, start, diff);
		else  // undo
			MidiTools.removeTape(t, end, diff);
		MainFrame.update(this);
	}

	protected void trim(Edit e, boolean exe) {
		long start = e.getOrigin().tick;
		long end = e.getDestination().tick;
		long diff = end - start;
		if (exe) { // remove notes then remove tape (shift remaining notes)
			for (MidiPair p : e.getNotes()) {
				t.remove(p.getOn());
				if (p.getOff() != null)
					t.remove(p.getOff());
			}
			MidiTools.removeTape(t, end, diff);
		} else { // undo: shift existing notes then paste deleted notes
			MidiTools.addTape(t, start, diff);
			for (MidiPair p : e.getNotes()) {
				t.add(p.getOn());
				if (p.getOff() != null)
					t.add(p.getOff());
			}
		}
		MainFrame.update(this);
	}

	protected final void mod(Edit e, boolean exe) {
		if (exe) { // remove original cc (on), add changed cc (off)
			MidiTools.delete(e.getNotes().getFirst().getOn(), t);
//			if (false == t.remove(e.getNotes().getFirst().getOn()))
//				RTLogger.log(this, "Missing: " + Midi.toString(e.getNotes().getFirst().getOn().getMessage()));
			t.add(e.getNotes().getFirst().getOff());
		} else { // undo, restore original cc (on)
			MidiTools.delete(e.getNotes().getFirst().getOff(), t);
			t.remove(e.getNotes().getFirst().getOff());
			t.add(e.getNotes().getFirst().getOn());
		}
		MainFrame.update(this);
	}

	protected void editAdd(ArrayList<MidiPair> replace) {
		selected.clear();
		for (MidiPair p : replace) {
			if (Midi.isNoteOn(p.getOn().getMessage()))
				selected.add(p); // ignore CCs
			track.getT().add(p.getOn());
			if (p.getOff() != null)
				track.getT().add(p.getOff());
		}
		MainFrame.update(this);
	}

	protected void editDel(ArrayList<MidiPair> notes) {
		for (MidiPair p: notes) {
				if (MidiTools.delete(p.getOn(), track.getT()) == false)
					RTLogger.warn(this, "Could not delete: " +
							Midi.toString(p.getOn().getMessage()) + " @ " + p.getOn().getTick());
				if (p.getOff() != null)
					MidiTools.delete(p.getOff(), track.getT());
            }
	}

	/** execute edit and add to undo stack */
	@Override public void push(Edit e) {
		stack.add(e);
		caret = stack.size() - 1;
		execute(e);
	}

	public Edit peek() {
		if (caret >= stack.size())
			return null;
		return stack.get(caret);
	}

	@Override public void redo() {
		if (stack.size() <= caret + 1)
			return;
		caret++;
		execute(stack.get(caret));
	}

	@Override public void selectNone() {
		selected.clear();
		MainFrame.update(this);
	}

	public Notes selectArea(long start, long end) {
		selected.clear();
		long tick;
		if (track.isDrums()) {
			scroll.populate();
			for (MidiPair p : scroll) {
				tick = p.getOn().getTick();
				if (tick < start) continue;
				if (tick >= end) break;
				selected.add(p);
			}
		}
		else {
			for (int i = 0; i < t.size(); i++) {
				tick = t.get(i).getTick();
				if (tick < start) continue;
				if (tick >= end) break;
				if (Midi.isNoteOn(t.get(i).getMessage()))
					selected.add(MidiTools.noteOff(t.get(i), t));
			}
		}
		repaint();
		return selected;
	}

	public void select(Notes notes) {
		selected.clear();
		selected.addAll(notes);
		repaint();
	}

	public Notes selectBar(boolean left) {
		long start = left ? track.getLeft() : track.getRight();
		long end = start + track.getBarTicks();
		selectArea(start, end);
		return selected;
	}

	@Override
	public Notes selectFrame() {
		long start = track.getLeft();
		long end = start + track.getWindow();
		selectArea(start, end);
		return selected;
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

