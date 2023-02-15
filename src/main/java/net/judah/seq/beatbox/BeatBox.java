package net.judah.seq.beatbox;

import static net.judah.seq.MidiTools.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import net.judah.drumkit.DrumType;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.midi.Midi;
import net.judah.seq.MidiPair;
import net.judah.seq.MidiTab;
import net.judah.seq.MusicBox;
import net.judah.seq.Prototype;
import net.judah.util.RTLogger;

public class BeatBox extends MusicBox {
	
	public static final int X_OFF = 5;
	private static final int RADIUS = 24;
    private static final int PADS = DrumType.values().length;
	
	private final int rowHeight;
	private final float totalWidth;
	private float unit;

    public BeatBox(final Rectangle r, BeatsSection view, MidiTab tab) {
    	super(view, r, tab);
		totalWidth = r.width;
    	rowHeight = (int)Math.ceil((r.height) / PADS);
		setOpaque(true);
    	setBackground(Pastels.MY_GRAY);
        setLayout(null);
        timeSig();
    }

    @Override public void timeSig() {
    	float old = unit;
		unit = totalWidth / (2f * track.getClock().getSteps());
		RTLogger.log(this, "TimeSig from " + old + " to " + unit);
		repaint();
	}
    
	@Override public long toTick(Point p) {
		return (long) (p.x / totalWidth * track.getWindow() + track.getLeft());
	}

	@Override public int toData1(Point p) {
		return DrumType.values()[p.y / rowHeight].getData1();
	}
    
    @Override public void paint(Graphics g) {
    	super.paint(g);
    	// background ovals
        int stepz = clock.getSteps();
        for (int x = 0; x < 2 * stepz ; x++) {
            for (int y = 0; y < PADS; y++) {
            	oval(g, (int) (x * unit), y, x % clock.getSubdivision() == 0 ? Pastels.BLUE :Color.WHITE);
            	if (x % stepz == 0) 
            		mini(g, (int) (x * unit), y);
            }
        }
        
        scroll.populate();
        ShortMessage on;
        int x, y;
        float window = track.getWindow();
        for (MidiPair p : scroll) {
        	on = (ShortMessage)p.getOn().getMessage();
			y = DrumType.index(on.getData1());
			x = (int) ( (p.getOn().getTick() / window) * totalWidth); 
			if (y >=0 && x >= 0) {
				if (selected.isBeatSelected(p))
					highlight(g, x, y, on.getData2());
				else
					oval(g, x, y, on.getData2()); 
			}
        }
    }
    
    private void oval(Graphics g, int x, int y, Color c) {
    	g.setColor(c);
    	g.fillOval(x + X_OFF, y * rowHeight + 2, RADIUS, RADIUS);
    }

    private void mini(Graphics g, int x, int y) {
    	g.setColor(Color.WHITE);
    	g.fillOval(x + X_OFF + RADIUS / 3, y * rowHeight + 2 + RADIUS / 3, RADIUS / 3, RADIUS / 3);
    }
    
    private void oval(Graphics g, int x, int y, int data2) {
    	oval(g, x, y, velocityColor(data2));
    }
    
    private void highlight(Graphics g, int x, int y, int data2) {
    	oval(g, x, y, highlightColor(data2));
    }

    @Override public void mouseClicked(MouseEvent evt) {
    	((BeatsTab)tab).setCurrent(view);

    	Prototype click = translate(evt.getPoint());
        MidiEvent existing = track.get(NOTE_ON, click.data1, click.tick);
        // TODO right mouse button menu?
        if (existing == null) {
        	MidiEvent create = new MidiEvent(Midi.create(NOTE_ON, track.getCh(), click.data1, 
        			(int) (track.getAmp() * 127f)), click.tick);
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
        repaint();
        MainFrame.qwerty();
    }

	
}
