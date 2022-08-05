package net.judah.tracker;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import lombok.Getter;
import net.judah.midi.JudahClock;
import net.judah.midi.NoteOn;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;

public class BeatBox extends JPanel implements MouseListener {

	private final int RADIUS = 24;
	
    @Getter private final CurrentBeat current;
    @Getter private static int rowHeight;
    private final int colWidth;
    private final Track track;
    
    
    public BeatBox(Track t, Rectangle r) {
    	setOpaque(true);
    	setBackground(Pastels.MY_GRAY);
        this.track = t;
        setMaximumSize(r.getSize());
		setPreferredSize(r.getSize());

        setLayout(null);
        colWidth = r.width  / JudahClock.getSteps();
        rowHeight = (int)Math.ceil((r.height - 30) / (GMDrum.Standard.length + 1f)) + 1;

        current = new CurrentBeat(t);

        ArrayList<BeatLabel> lbls = current.createLabels();
        for (int i = 0; i < lbls.size(); i++) {
            BeatLabel lbl = lbls.get(i);
            lbl.setBounds(i * colWidth + 3, 1, 26, 26);
            add(lbl);
        }
        addMouseListener(this);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;

        Pattern pat = track.getCurrent();
        DrumKit kit = track.getKit();
        Color color;
        for (int x = 0; x < track.getSteps(); x++) {
            color = x % track.getDiv() == 0 ? Pastels.BLUE :Color.WHITE;
            for (int y = 0; y < kit.size(); y++) {
            	
            	if (pat.get(x, kit.get(y).getData1()) == null)
                    g2d.setPaint(color);
                else {
                	g2d.setPaint(Pastels.forType(pat.get(x, kit.get(y).getData1())));
                }
                g2d.fillOval(x * colWidth + 3, y * rowHeight + rowHeight + 5, RADIUS, RADIUS);
            }
        }
    }

    private Point translate(Point p) {
        if (p.y < rowHeight + 2) return new Point(p.x / colWidth, -1);
        return new Point(p.x / colWidth,  (p.y - rowHeight) / rowHeight);
    }

    @Override public void mouseClicked(MouseEvent evt) {
        Point xy = translate(evt.getPoint());
        if (xy.y < 0) { // label row clicked, user wants to hear this step
            if (xy.x >= 0 && xy.x< JudahClock.getSteps())
                if (SwingUtilities.isRightMouseButton(evt))
                    for (Track t : JudahClock.getInstance().getTracks())
                        t.step(xy.x);
                else
                    track.step(xy.x);
            return;
        }
        else {
        	try {
	        	int data1 = track.getKit().get(xy.y).getData1();
		        Notes note = track.getCurrent().get(xy.x);
		        if (note == null) {
		        	track.getCurrent().put(xy.x, new Notes(new NoteOn(track.getCh(), data1)));
		        } else {
		        	if (note.find(data1) == null) {
		        		note.add(new NoteOn(track.getCh(), data1));
		        	} else {
		        		note.remove(note.find(data1));
		        		if (note.isEmpty())
		        			track.getCurrent().remove(xy.x);
		        	}
		        }
		        repaint();
        	} catch (InvalidMidiDataException e) {
        		RTLogger.warn(this, e);
        	}
        }
    }

    @Override public void mousePressed(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }

	public void step(int step) {
		current.setActive(step);
	}
}
