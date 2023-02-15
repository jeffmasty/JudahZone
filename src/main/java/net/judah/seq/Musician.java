package net.judah.seq;

import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

public interface Musician extends MouseListener, MouseWheelListener, MouseMotionListener, MidiConstants, KeyListener {
	char DELETE = '\u007F';
	
	void timeSig();
	long toTick(Point p);
	int toData1(Point p);
	Prototype translate(Point p) ;
//	float tickRatio(long tick, long measure);
	Gate getGate();
	long quantize(long tick, Gate type, int resolution);
	public void delete();
	public void copy();
	public void paste();

}
