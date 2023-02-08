package net.judah.seq;

import static java.awt.event.KeyEvent.*;

import java.awt.Point;
import java.awt.event.*;
import java.util.Stack;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.util.RTLogger;

@RequiredArgsConstructor
public abstract class Musician implements MouseListener, MouseWheelListener, MouseMotionListener, MidiConstants, KeyListener {
	private final char DELETE = '\u007F';
	
	protected final MidiTrack track;
	protected final JudahClock clock;
	protected final Steps steps;
	protected final MidiView view;
	protected final MidiTab tab;

	@Getter protected Prototype on;
	@Getter protected boolean drag;
	@Getter protected final Notes selected;
	@Getter protected final Stack<Edit> edits = new Stack<>();
	
	public Musician(MidiView view, MidiTab tab) {
		this.view = view;
		this.selected = view.getSelected();
		this.track = view.getTrack();
		this.clock = track.getClock();
		this.steps = view.getSteps();
		this.tab = tab;
	}
	
	public abstract long toTick(Point p);
	public abstract int toData1(Point p);
	public final Prototype translate(Point p) {
		return new Prototype(toData1(p), toTick(p));
	}

	@Override public void mouseDragged(MouseEvent e) { // piano sub/super
		
		if (selected.isEmpty()) {
			// TODO select region ? 
		}
		else if (on == null) {
			on = translate(e.getPoint());
			drag = true;
		}
		
	}

	@Override public void mouseReleased(MouseEvent e) {	
		
		if (drag) { // paste selection at new point
			Prototype trans = translate(e.getPoint());
			if (on == null) return; // error;
			int delta1 = on.data1 - trans.data1;
			long deltaTick = on.tick - trans.tick;
			for (MidiPair p : selected) {
				
			}
			drag = false; 
			on = null;
		}
	}

	
	public void delete() {
		int before = track.getT().size();
		for (MidiPair p : selected) {
			MidiTools.delete(p.getOn(), track.getT());
			if (p.getOff() != null)
				MidiTools.delete(p.getOff(), track.getT());
            }
		selected.clear();
		view.getGrid().repaint();
		// RTLogger.log(this, track.getName() + " DELETE! before: " + before + " after: " + track.getT().size()); 
	}


	public void copy() {
		tab.getClipboard().clear();
		for (MidiPair p : selected) 
			tab.getClipboard().add(p);
	}

	public void paste() {
		selected.clear();
		for (MidiPair p : tab.getClipboard()) {
			MidiTools.interpolate(p.getOn(), track);
			if (p.getOff() != null)
				MidiTools.interpolate(p.getOff(), track);	
			selected.add(p);
		}
		view.getGrid().repaint();
	}

	@Override public void keyTyped(KeyEvent e) {
		//RTLogger.log(this, track.getName() + " typed " + e.getKeyChar());
		if (e.getKeyChar() == DELETE)  
			delete();
		
        switch(e.getKeyCode()) {
        	case VK_DELETE: RTLogger.log(this, "DELETE!"); delete(); break;
        	case VK_UP: RTLogger.log(this, "UP");break; 
            case VK_DOWN: RTLogger.log(this, "DOWN");break;  
            case VK_LEFT: RTLogger.log(this, "LEFT");break;  
            case VK_RIGHT: RTLogger.log(this, "RIGHT");break;  
            //default: RTLogger.log(this, "unknown: " + e);
        }
	}
	@Override public void keyPressed(KeyEvent e) {RTLogger.log(this, track.getName() + " pressed " + e.getKeyChar()); }
	@Override public void keyReleased(KeyEvent e) { RTLogger.log(this, track.getName() + " released " + e.getKeyChar());}
	@Override public final void mouseEntered(MouseEvent e) {MainFrame.qwerty();}
	@Override public void mouseExited(MouseEvent e) { }
	@Override public void mouseClicked(MouseEvent e) { } // drummer sub
	@Override public void mouseMoved(MouseEvent e) { }
	@Override public void mousePressed(MouseEvent e) { on = translate(e.getPoint());} // piano sub
	@Override public void mouseWheelMoved(MouseWheelEvent wheel) { 
		//	boolean up = wheel.getPreciseWheelRotation() < 0;
		//	int velocity = menu.getVelocity().getValue() + (up ? 5 : -1); // ??5
		//	if (velocity < 0)
		//		velocity = 0;
		//	if (velocity > 100)
		//		velocity = 99;
		//	menu.getVelocity().setValue(velocity);
	}
}
