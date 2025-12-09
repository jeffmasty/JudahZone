package net.judah.seq;

import java.awt.Point;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

import net.judah.api.Signature;
import net.judah.seq.track.MidiTrack;

/** User interaction with a Notes Track */
public interface Musician extends MouseListener, MouseWheelListener, MouseMotionListener {

	MidiTrack getTrack();
	void timeSig(Signature value);
	void velocity(boolean up);
	long toTick(Point p);
	int toData1(Point p);
	Prototype translate(Point p) ;
	void dragStart(Point p);
	void drag(Point p);
	void drop(Point p);
	Notes selectArea(long start, long end, int low, int high);
	Notes selectFrame();
	void selectNone();
	Notes getSelected();
	void delete();
	void copy();

}
