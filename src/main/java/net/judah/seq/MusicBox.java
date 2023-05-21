package net.judah.seq;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;

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
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public abstract class MusicBox extends JPanel implements Musician {
	private static final char DELETE = '\u007F';
	private static final char ESCAPE = '\u001b';

	protected static enum DragMode { CREATE, TRANSLATE, SELECT }
	protected static final Composite transparent = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
	
	protected final MidiTrack track;
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
	private ArrayList<MidiPair> dragging = new ArrayList<>();
	private Prototype recent;
	
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
		if (mode != null) {
			switch(mode) {
			case CREATE: 
				repaint();
				break;
			case SELECT: 
				if (mouse.getPoint().x < drag.x)
					drag = null; // veto inverse squares
				if (mouse.getPoint().y < drag.y)
					drag = null;
				repaint();
				break;
			case TRANSLATE: 
				drag(mouse.getPoint());
				break;
			}
		}
	}
	
	@Override public void delete() {
		push(new Edit(Type.DEL, selected));
	}

	@Override public void copy() {
		tab.getClipboard().clear();
		for (MidiPair p : selected) 
			tab.getClipboard().add(MidiTools.zeroBase(p, track.getLeft()));
	}

	@Override public void paste() {
		ArrayList<MidiPair> notes = new ArrayList<>();
		long offset = track.getLeft();
		for (MidiPair p : tab.getClipboard()) {
			MidiEvent off = null;
			if (p.getOff() != null)
				off = new MidiEvent(p.getOff().getMessage(), p.getOff().getTick() + offset); 
			notes.add(new MidiPair(new MidiEvent(p.getOn().getMessage(), p.getOn().getTick() + offset), off));
		}
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
			switch(intchar) {
			case 1: selectFrame(); break; // ctrl-a
			case 3: copy(); break; // ctrl-c
			case 22: paste(); break; // ctrl-v
			case 26: undo(); break; // ctrl-z
			default: RTLogger.log(this, "key: " + ch + "=" + intchar + ";" + e.getModifiersEx());
			}
		}
		else if (intchar == 10)
			track.setFrame(track.getFrame() + 1);
		else 
			RTLogger.log(this, "key: " + ch + "=" + intchar + ";" + e.getModifiersEx());
	}
	// public void mouseReleased(MouseEvent e) { } subclass implement
	// public void mousePressed(MouseEvent e) { } subclass implement
	@Override public final void mouseEntered(MouseEvent e) {MainFrame.qwerty();}
	@Override public void mouseMoved(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }
	@Override public final void mouseClicked(MouseEvent e) { } 
	@Override public final void mouseWheelMoved(MouseWheelEvent wheel) { }
	@Override public final void keyPressed(KeyEvent e) {RTLogger.log(this, track.getName() + " pressed " + e.getKeyChar()); }
	@Override public final void keyReleased(KeyEvent e) { RTLogger.log(this, track.getName() + " released " + e.getKeyChar());}
	// if (e.getKeyCode() == CTRL_DOWN_MASK) switch(e.getKeyCode()) {
	//        	case VK_UP: case VK_DOWN: case VK_LEFT: VK_RIGHT: break;  
	//	MouseWheel: boolean up = wheel.getPreciseWheelRotation() < 0;
	//	int velocity = menu.getVelocity().getValue() + (up ? 5 : -1); // ??5
	//	if (velocity < 0)  velocity = 0;
	//	if (velocity > 100)  velocity = 99;
	//	menu.getVelocity().setValue(velocity);


	protected void transpose(ArrayList<MidiPair> notes, Prototype destination) {
		editDel(notes);
		ArrayList<MidiPair> replace = new ArrayList<>();
		for (MidiPair note : notes) 
			replace.add(Transpose.compute(note, destination, track));
		editAdd(replace);
	}
	
	private void decompose(ArrayList<MidiPair> notes, Prototype destination) {
		ArrayList<MidiPair> delete = new ArrayList<>();
		for (MidiPair note : notes)
			delete.add(Transpose.compute(note, destination, track));
		editDel(delete);
		editAdd(notes);
		click = null;
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
			click = null;
			break;
		case TRANS:
			if (track.isActive())
				Constants.execute(new Panic(track.getMidiOut(), track.getCh()));
			transpose(e.getNotes(), e.getDestination());
			click = null;
			break;
		default: // GAIN
			break;
		}
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
				Constants.execute(new Panic(track.getMidiOut(), track.getCh()));
			decompose(e.getNotes(), e.getDestination());
			break;
		default:
			break;
		}
		caret--;
		return true;
	}

	protected void editAdd(ArrayList<MidiPair> list) {
		selected.clear(); // add zero-based selection
		for (MidiPair p : list) {
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

	public void selectFrame() {
		long start = track.getLeft();
		long end = start + track.getWindow();
		selectArea(start, end);
	}
	
	public void selectStep(int step) {
		long div = track.getResolution() / clock.getSubdivision();
		long start = track.getLeft() + (step * div);
		long end = start + div;
		selectArea(start, end, 0, 127);
	}

	@Override
	public void selectArea(long start, long end, int low, int high) {
		selected.clear();
		for (int i = 0; i < t.size(); i++) {
			long tick = t.get(i).getTick();
			if (tick < start) continue;
			if (tick >= end) break;
			if (Midi.isNoteOn(t.get(i).getMessage())) {
				ShortMessage on = (ShortMessage)t.get(i).getMessage();
				int data1 = on.getData1();
				if (data1 >= low && data1 <= high)
					selected.add(track.isDrums() ? new MidiPair(t.get(i), null) : MidiTools.noteOff(t.get(i), t));
			}
		}
		repaint();
	}

	//////// Drag and Drop /////////
	@Override public void dragStart(Point mouse) {
		mode = DragMode.TRANSLATE;
		dragging.clear();
		selected.forEach(p -> dragging.add(new MidiPair(p)));
		click = recent = new Prototype(toData1(mouse), track.quantize(toTick(mouse)));
	}
	
	@Override public void drag(Point mouse) {
		Prototype now = new Prototype(toData1(mouse), track.quantize(toTick(mouse)));
		if (now.equals(recent)) // hovering
			return; 
		// note or step changed, move from most recent drag spot
		Prototype destination = new Prototype(now.getData1() - recent.getData1(), 
				((now.getTick() - recent.getTick()) % track.getWindow()) / track.getStepTicks());
		recent = now;
		transpose(selected, destination);
	}
	
	@Override public void drop(Point mouse) {
		// delete selected, create undo from start/init
		editDel(selected);
		Edit e = new Edit(Type.TRANS, dragging);
		Prototype now = new Prototype(toData1(mouse), track.quantize(toTick(mouse)));
		Prototype destination = new Prototype(now.getData1() - click.getData1(), 
				((now.getTick() - click.getTick()) % track.getWindow()) / track.getStepTicks());
		e.setDestination(destination);
		push(e);
	}
	
}
