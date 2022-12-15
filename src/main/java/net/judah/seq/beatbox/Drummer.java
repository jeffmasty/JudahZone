package net.judah.seq.beatbox;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import net.judah.seq.MidiTrack;
import net.judah.seq.MidiView;

public class Drummer extends MouseAdapter {

	private final MidiView view;
	private final MidiTrack track;
	private final BeatBox grid;
	private final int rowHeight, colWidth;
	
	public Drummer(MidiView view, BeatBox grid, Rectangle size) {
		this.view = view;
		this.track = view.getTrack();
		this.grid = grid;

		// TODO 
		this.rowHeight = size.height;
		this.colWidth = size.width;
	}
	
	private Point translate(Point p) {
        if (p.y < rowHeight + 2) return new Point(p.x / colWidth, -1);
        return new Point(p.x / colWidth,  (p.y - rowHeight) / rowHeight);
    }

    @Override public void mouseClicked(MouseEvent evt) {
        Point xy = translate(evt.getPoint());
        if (xy.y < 0) { // label row clicked
        		return;
        }
//        else {
//        	try { // see also Pattern.setValueAt(,,)
//	        	int data1 = DrumType.values()[xy.y].getDat().getData1();
//		        Notes note = track.getCurrent().get(xy.x);
//		        if (note == null) {
//		        	track.getCurrent().put(xy.x, new Notes(new NoteOn(track.getCh(), data1)));
//		        } else {
//		        	if (note.find(data1) == null) {
//		        		note.add(new NoteOn(track.getCh(), data1));
//		        	} else if (SwingUtilities.isRightMouseButton(evt)) {
//		        		Midi m = note.find(data1);
//		        		Midi replace = velocity(m);
//		        		note.remove(m);
//		        		note.add(replace);
//		        	}
//		        	else {	
//		        		note.remove(note.find(data1));
//		        		if (note.isEmpty())
//		        			track.getCurrent().remove(xy.x);
//		        	}
//		        }
//		        grid.repaint();
//        	} catch (InvalidMidiDataException e) {
//        		RTLogger.warn(this, e);
//        	}
//        }
    }


	
}
