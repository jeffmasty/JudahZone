package net.judah.seq.beatbox;

import static net.judah.seq.MidiTools.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.sound.midi.ShortMessage;

import net.judah.drumkit.DrumType;
import net.judah.gui.Pastels;
import net.judah.seq.Measure;
import net.judah.seq.MidiPair;
import net.judah.seq.MidiTab;
import net.judah.seq.MusicGrid;
import net.judah.seq.Musician;

public class BeatBox extends MusicGrid {
	public static final int X_OFF = 5;
	private final int RADIUS = 24;
    private final int PADS = DrumType.values().length;
	
	private final BeatSteps steps;
    private Drummer drummer;
	private final int rowHeight;
    private int unit;

    final ArrayList<MidiPair> selected;
    
    public BeatBox(final Rectangle r, BeatsSection view, BeatSteps steps, MidiTab tab) {
    	super(view.getTrack(), r);
        this.steps = steps;
        selected = new Measure(track);
		rowHeight = (int)Math.ceil((r.height) / PADS);
    	setOpaque(true);
    	setBackground(Pastels.MY_GRAY);
        setLayout(null);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        unit = (int)steps.getUnit();
        float barTicks = track.getBarTicks();
        int stepz = clock.getSteps();
        for (int x = 0; x < 2 * stepz ; x++) {
            for (int y = 0; y < PADS; y++) {
            	oval(g, x, y, x % clock.getSubdivision() == 0 ? Pastels.BLUE :Color.WHITE);
            	if (x % stepz == 0) 
            		mini(g, x, y);
            }
        }
        
        scroll.populate();
        
        //RTLogger.log(this, "Painting " + scroll.size());
        ShortMessage on;
        int x, y;
        for (MidiPair p : scroll) {
        	if (p.getOn().getMessage() instanceof ShortMessage == false)
        		continue;
        	on = (ShortMessage)p.getOn().getMessage();
			y = DrumType.index(on.getData1());
			x = (int) (p.getOn().getTick() / barTicks * stepz);
			if (y >=0 && x >= 0) {
				if (drummer.getSelected().isBeatSelected(p))
					highlight(g, x, y, on.getData2());
				else
					oval(g, x, y, on.getData2()); 
			}
        }
    }
    
    private void oval(Graphics g, int x, int y, Color c) {
    	g.setColor(c);
    	g.fillOval(x * unit + X_OFF, y * rowHeight + 2, RADIUS, RADIUS);
    }

    private void mini(Graphics g, int x, int y) {
    	g.setColor(Color.WHITE);
    	g.fillOval(x * unit + X_OFF + RADIUS / 3, y * rowHeight + 2 + RADIUS / 3, RADIUS / 3, RADIUS / 3);
    }
    
    private void oval(Graphics g, int x, int y, int data2) {
    	oval(g, x, y, velocityColor(data2));
    }
    
    private void highlight(Graphics g, int x, int y, int data2) {
    	oval(g, x, y, highlightColor(data2));
    }

	public void setHandler(Drummer drummer) {
		this.drummer = drummer;
		addMouseListener(drummer);
        addMouseMotionListener(drummer);
        addMouseMotionListener(drummer);
	}

	@Override
	public Musician getMusician() {
		return drummer;
	}

	@Override
	public void timeSig() {
		
		
	}

}
