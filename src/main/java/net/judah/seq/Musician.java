package net.judah.seq;

import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

import net.judah.midi.Signature;

public interface Musician extends MouseListener, MouseWheelListener, MouseMotionListener, MidiConstants, KeyListener {
	
	void timeSig(Signature value);
	long toTick(Point p);
	int toData1(Point p);
	Prototype translate(Point p) ;
	void push(Edit e);
	boolean undo();
	boolean redo();
	void delete();
	void copy();
	void paste();
	void dragStart(Point p);
	void drag(Point p);
	void drop(Point p);
	void selectArea(long start, long end, int low, int high);
	void selectNone();
	
}
