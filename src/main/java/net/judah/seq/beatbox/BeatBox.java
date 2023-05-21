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
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiPair;
import net.judah.seq.MidiTab;
import net.judah.seq.MusicBox;
import net.judah.seq.Prototype;

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
		unit = totalWidth / (2f * track.getClock().getSteps());
		repaint();
	}
    
	@Override public long toTick(Point p) {
		return track.quantize((long) (p.x / totalWidth * track.getWindow() + track.getLeft()));
	}

	@Override public int toData1(Point p) {
		int idx = p.y / rowHeight;
		if (idx >= PADS)
			idx = PADS - 1;
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
        
        long offset = track.getLeft();
        scroll.populate();
        ShortMessage on;
        int x, y;
        float window = track.getWindow();
        for (MidiPair p : scroll) {
        	on = (ShortMessage)p.getOn().getMessage();
			y = DrumType.index(on.getData1());
			x = (int) ( (  (p.getOn().getTick()-offset) / window) * totalWidth); 
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

    @Override public void mousePressed(MouseEvent mouse) {
    	((BeatsTab)tab).setCurrent(view);
    	click = translate(mouse.getPoint());
		MidiEvent existing = track.get(NOTE_ON, click.data1, click.tick);
		if (mouse.isShiftDown()) { // drag-n-select;
			drag = mouse.getPoint();
			mode = DragMode.SELECT;
		}
		else if (existing == null) {
			drag = mouse.getPoint();
			mode = DragMode.CREATE;
		}
		else {
			MidiPair pair = new MidiPair(existing, null);
			if (mouse.isControlDown()) {
				if (selected.contains(pair))
					selected.remove(pair);
				else 
					selected.add(pair);
			}
			else {
				if (selected.contains(pair)) {
					dragStart(mouse.getPoint()); // already selected, start drag-n-drop
				}
				else { // simple select click
					selected.clear();
					selected.add(pair);
				}
			}
		}
		repaint();
    }
    
	@Override public void mouseReleased(MouseEvent mouse) {
		if (mode != null) {
			switch(mode) {
			case CREATE: 
				MidiEvent create = new MidiEvent(Midi.create(NOTE_ON, track.getCh(), click.data1, 
						(int) (track.getAmp() * 127f)), click.tick);
				push(new Edit(Type.NEW, new MidiPair(create, null)));
				break;
			case SELECT: 
				Prototype a = translate(drag);
				Prototype b = translate(mouse.getPoint());
				drag = null;
				selectArea(a.getTick(), b.getTick(), a.getData1(), b.getData1());
				break;
			case TRANSLATE: 
				drop(mouse.getPoint());
				break;
			}
			mode = null;
			repaint();
			click = null;
		}
        MainFrame.qwerty();
	}


	
}
