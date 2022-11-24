package net.judah.tracker.edit;

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
import net.judah.api.Midi;
import net.judah.drumz.DrumType;
import net.judah.midi.NoteOn;
import net.judah.tracker.DrumTrack;
import net.judah.tracker.Notes;
import net.judah.tracker.Pattern;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;

public class BeatBox extends JPanel implements MouseListener {

	private final int RADIUS = 23;
	// TODO 1/8th note highlight if div > 5
    @Getter private final CurrentBeat current;
    @Getter private static int rowHeight;
    private final int pnlWidth;
    private int colWidth;
    private ArrayList<BeatLabel> lbls;
    private final DrumTrack track;
    private final int length = DrumType.values().length;
    
    public BeatBox(DrumTrack t, Rectangle r) {
    	
    	setOpaque(true);

    	setBackground(Pastels.MY_GRAY);
        this.track = t;
        setMaximumSize(r.getSize());
		setPreferredSize(r.getSize());
		pnlWidth = r.width;
        setLayout(null);
        colWidth();
        rowHeight = (int)Math.ceil((r.height - 30) / (length + 1f));

        current = new CurrentBeat(t);

        lbls = current.createLabels();
        addLbls();
        addMouseListener(this);
    }

    @Override
    public void paint(Graphics g) {
    	
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;

        Pattern pat = track.getCurrent();
        Color color;
        int data1;
        for (int x = 0; x < track.getSteps(); x++) {
            color = x % track.getDiv() == 0 ? Pastels.BLUE :Color.WHITE;
            for (int y = 0; y < length; y++) {
            	data1 = DrumType.values()[y].getDat().getData1();
            	if (pat.getNote(x, data1) == null)
                    g2d.setPaint(color);
                else {
                	g2d.setPaint(Pastels.forType(pat.getNote(x, data1)));
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
        if (xy.y < 0) { // label row clicked
        		return;
        }
        else {
        	try { // see also Pattern.setValueAt(,,)
	        	int data1 = DrumType.values()[xy.y].getDat().getData1();
		        Notes note = track.getCurrent().get(xy.x);
		        if (note == null) {
		        	track.getCurrent().put(xy.x, new Notes(new NoteOn(track.getCh(), data1)));
		        } else {
		        	if (note.find(data1) == null) {
		        		note.add(new NoteOn(track.getCh(), data1));
		        	} else if (SwingUtilities.isRightMouseButton(evt)) {
		        		Midi m = note.find(data1);
		        		Midi replace = velocity(m);
		        		note.remove(m);
		        		note.add(replace);
		        	}
		        	else {	
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


    private Midi velocity(Midi m) {
    	int i = 2 + (int)(m.getData2() / 32f);
    	if (i > 4) i = 1;
    	RTLogger.log(this, "velocity " + i);
    	return Midi.create(m.getCommand(), m.getChannel(), m.getData1(), i * 32 - 1);
    }
    
	public void step(int step) {
		if (step >= 0)
			current.setActive(step);
	}
	
	private void colWidth() {
		colWidth = pnlWidth / track.getSteps() - 2;
	}
	
	public void measure() {
		if (lbls != null)
			for (BeatLabel l : lbls) {
				remove(l);
			}
		lbls = current.createLabels();
		addLbls();
		invalidate();
	}
	
	private void addLbls() {
        for (int i = 0; i < lbls.size(); i++) {
            BeatLabel lbl = lbls.get(i);
            lbl.setBounds(i * colWidth + 3, 1, 26, 26);
            add(lbl);
        }
	}
}
