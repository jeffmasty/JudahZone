package net.judah.seq.beatbox;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.sound.midi.MidiEvent;

import net.judah.drumkit.DrumType;
import net.judah.gui.MainFrame;
import net.judah.midi.Midi;
import net.judah.seq.MidiPair;
import net.judah.seq.Musician;
import net.judah.seq.Prototype;

public class Drummer extends Musician {

	private final BeatBox grid;
	private final int rowHeight, colWidth;
	
	public Drummer(Rectangle size, BeatsSection view, BeatBox grid, BeatsTab tab, BeatSteps steps) {
		super(view, tab);
		this.grid = grid;
		this.rowHeight = size.height / DrumType.values().length;
		this.colWidth = size.width / (int)steps.getTotal();
	}

	@Override
	public long toTick(Point p) {
        int x = p.x / colWidth;
        long base = x < clock.getSteps() ? track.getLeft() : track.getRight();
//        if (track.isEven()) 
//        	base = 
//        	measure = x < clock.getSteps() ? track.getCurrent() : track.getNext();
//        else 
//        	measure = x < clock.getSteps() ? track.getPrevious() : track.getCurrent();
        int step = x % clock.getSteps();
        return base + step * (track.getResolution() / clock.getSubdivision());
	}

	@Override
	public int toData1(Point p) {
		return DrumType.values()[p.y / rowHeight].getData1();
	}

    @Override public void mouseClicked(MouseEvent evt) {
    	((BeatsTab)tab).setCurrent(view);

    	Prototype click = translate(evt.getPoint());
        MidiEvent existing = track.get(NOTE_ON, click.data1, click.tick);
        // TODO right mouse button menu?
        if (existing == null) {
        	MidiEvent create = new MidiEvent(Midi.create(NOTE_ON, track.getCh(), click.data1, 
        			Math.round(track.getAmplification() * 1.27f)), click.tick);
        	track.getT().add(create);
        	selected.clear();
        	selected.add(new MidiPair(create, null));
        }
        else {
        	MidiPair pair = new MidiPair(existing, null);
        	if (evt.isControlDown()) {
        		if (selected.isBeatSelected(pair)) {
        			selected.remove(pair);
        		}
        		else {
        			selected.add(pair);
        		}
        	}
        	else {
        		selected.clear();
        		selected.add(pair);
        	}
        		
        }
        grid.repaint();
        MainFrame.qwerty();
    }
    
//    private Midi velocity(Midi m) {
//    	int i = 2 + (int)(m.getData2() / 32f);
//    	if (i > 4) i = 1;
//    	RTLogger.log(this, "velocity " + i);
//    	return Midi.create(m.getCommand(), m.getChannel(), m.getData1(), i * 32 - 1);
//    }


    
}
