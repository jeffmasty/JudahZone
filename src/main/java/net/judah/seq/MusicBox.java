package net.judah.seq;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.seq.Edit.Type;
import net.judah.seq.track.MidiTrack;
import net.judah.util.RTLogger;

public abstract class MusicBox extends JPanel implements Musician {
	private static final char DELETE = '\u007F';
	private static final char ESCAPE = '\u001b';
	protected static final Composite transparent = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
	public static enum DragMode { CREATE, TRANSLATE, SELECT }

	@Getter protected final MidiTrack track;
	protected final Track t;
	protected final JudahClock clock;
	protected final MidiTab tab;
	protected final MidiView view;
	protected final Measure scroll;
	/** undo/redo */
	protected ArrayList<Edit> stack = new ArrayList<>();
	private int caret;
	/** absolute notes */
	@Getter protected final Notes selected = new Notes();
	protected Prototype click;
	protected Point drag = null;
	protected DragMode mode = null;

	public MusicBox(MidiView view, Rectangle r, MidiTab tab) {
		this.track = view.getTrack();
		this.t = track.getT();
		this.clock = track.getClock();
		this.tab = tab;
		this.view = view;
		scroll = new Measure(track);
		setBounds(r);
		setMaximumSize(r.getSize());
		setPreferredSize(r.getSize());
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
		tab.getClipboard().copy(selected, track);
	}

	@Override public void paste() { // TODO differences in track Resolution
		List<MidiPair> notes = tab.getClipboard().paste(track);
		push(new Edit(Type.NEW, notes));
	}

	@Override public void keyTyped(KeyEvent e) {
		char ch = e.getKeyChar();
		int intchar = ch;

		if (ch == DELETE)
			delete();
		else if (ch == ESCAPE)
			selectNone();
		else if (ch == ' ')
			track.setActive(!track.isActive());
		else if (e.getModifiersEx() == 128) {
			switch (intchar) {
				case 1: selectFrame(); break; 		// ctrl-a
				case 3: copy(); break; 				// ctrl-c
				case 4: selectNone(); break; 		// ctrl-d
				case 12: new Duration(this); break; // ctrl-l
				case 18: undo(); break; 			// ctrl-r
				case 19: track.save(); break; 		// ctrl-s
				case 20: new Transpose(track, this); break; // ctrl-t
				case 22: paste(); break; 			// ctrl-v
				case 26: undo(); break; 			// ctrl-z
			default: RTLogger.log(this, "key: " + ch + "=" + intchar + ";" + e.getModifiersEx());
			}
		}
		else
			RTLogger.log(this, "key: " + ch + "=" + intchar + ";" + e.getModifiersEx());
	}

	@Override public final void mouseEntered(MouseEvent e) {MainFrame.qwerty();}
	@Override public void mouseMoved(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }
	@Override public final void mouseClicked(MouseEvent e) { }
	@Override public final void keyPressed(KeyEvent e) {RTLogger.log(this, track.getName() + " pressed " + e.getKeyChar()); }
	@Override public final void keyReleased(KeyEvent e) { RTLogger.log(this, track.getName() + " released " + e.getKeyChar());}
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

}

