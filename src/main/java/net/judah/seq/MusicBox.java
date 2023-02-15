package net.judah.seq;


import static java.awt.event.KeyEvent.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Stack;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.util.RTLogger;

public abstract class MusicBox extends JPanel implements Musician {

	protected final MidiTrack track;
	protected final JudahClock clock;
	protected final MidiTab tab;
	protected final MidiView view;
	protected final Measure scroll;
	protected final Notes selected = new Notes();
	protected final Stack<Edit> edits = new Stack<>();
	protected Prototype on;
	@Getter protected boolean drag;
	
	public MusicBox(MidiView view, Rectangle r, MidiTab tab) {
		this.track = view.getTrack();
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

	@Override public Gate getGate() {
		return (Gate)view.getMenu().getGate().getSelectedItem();
	}

	// TODO odd subdivision
	@Override
	public long quantize(long tick, Gate type, int resolution) {
		switch(type) {
		case SIXTEENTH: return tick - tick % (resolution / 4);
		case EIGHTH: return tick - tick % (resolution / 2);
		case QUARTER: return tick - tick % resolution;
		case HALF: return tick - tick % (2 * resolution);
		case WHOLE: return tick - tick % (4 * resolution);
		case MICRO: return tick - tick % (resolution / 8);
		case RATCHET: // approx MIDI_24
		default: // NONE
			return tick;
		}
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
				// todo
			}
			drag = false; 
			on = null;
		}
	}

	// piano subclasses
	@Override public void mousePressed(MouseEvent e) { 
		on = translate(e.getPoint());} 

	@Override public final void mouseEntered(MouseEvent e) {MainFrame.qwerty();}
	@Override
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


	@Override
	public void copy() {
		tab.getClipboard().clear();
		for (MidiPair p : selected) 
			tab.getClipboard().add(p);
	}

	@Override
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
	@Override public void mouseExited(MouseEvent e) { }
	@Override public void mouseClicked(MouseEvent e) { } // drummer sub
	@Override public void mouseMoved(MouseEvent e) { }
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
