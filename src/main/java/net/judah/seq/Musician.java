package net.judah.seq;

import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

import net.judah.api.Signature;

/** User interaction with a Midi Track */
public interface Musician extends MidiConstants, KeyListener, MouseListener, MouseWheelListener, MouseMotionListener {

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
	void transpose(ArrayList<MidiPair> notes, Prototype destination);
	void decompose(Edit e);

}
