package net.judah.seq.beatbox;

import static net.judah.seq.MidiTools.velocityColor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JPanel;

import net.judah.drumkit.DrumType;
import net.judah.gui.Pastels;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.seq.MidiTrack;
import net.judah.seq.MidiView;
import net.judah.seq.Note;
import net.judah.seq.Snippet;
import net.judah.seq.Steps;
import net.judah.util.RTLogger;

public class BeatBox extends JPanel {
	private final int RADIUS = 24;
    private final int PADS = DrumType.values().length;
	
	private final MidiTrack track;
	private final JudahClock clock;
    private final Steps steps;
    private final Snippet notes;
    private final Drummer drummer;
    private final int rowHeight;
    private int unit;
    
    public BeatBox(Rectangle r, MidiView view, Steps steps) {
    	setBounds(r);
        this.track = view.getTrack();
        this.clock = track.getClock();
        this.steps = view.getSteps();
        this.notes = view.getSnippet();

        setMaximumSize(r.getSize());
		setPreferredSize(r.getSize());
		
		rowHeight = (int)Math.ceil((r.height) / PADS);
    	setOpaque(true);
    	setBackground(Pastels.MY_GRAY);
        setLayout(null);
        drummer = new Drummer(view, this, r);
        addMouseListener(drummer);
        addMouseMotionListener(drummer);
        addMouseMotionListener(drummer);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        unit = steps.getUnit();
        for (int x = 0; x < 2 * clock.getSteps(); x++) {
            g.setColor(x % clock.getSubdivision() == 0 ? Pastels.BLUE :Color.WHITE);
            for (int y = 0; y < PADS; y++) {
            	oval(g, x, y);
            }
        }
		track.publishView(notes);

		Note note = notes.poll();
		while (note != null) {
			int y = DrumType.index(note.data1);
			int x = (int) (note.onFactor * steps.getTotal());			
			if (y >=0 && x >= 0) {
				g.setColor(velocityColor(note.getData2()));
				oval(g, x, y); 
			}
			note = notes.poll();
		}
    }
    
    private void oval(Graphics g, int x, int y) {
    	g.fillOval(x * unit, y * rowHeight + 15, RADIUS, RADIUS);
    }
    
    private Midi velocity(Midi m) {
    	int i = 2 + (int)(m.getData2() / 32f);
    	if (i > 4) i = 1;
    	RTLogger.log(this, "velocity " + i);
    	return Midi.create(m.getCommand(), m.getChannel(), m.getData1(), i * 32 - 1);
    }
	
}
