package net.judah.beatbox;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import lombok.Getter;
import net.judah.beatbox.Beat.Type;
import net.judah.clock.JudahClock;
import net.judah.util.BeatLabel;
import net.judah.util.CurrentBeat;
import net.judah.util.Pastels;

public class GridView extends JPanel implements MouseListener {

	public static final int ROW_HEIGHT = 24;
	
    @Getter private final CurrentBeat current;

    @Getter private final int rowHeight;
    private final int colWidth;

    public GridView(Rectangle r) {
        setOpaque(false);
        setLayout(null);

        colWidth = r.width  / JudahClock.getSteps();
        rowHeight = (int)Math.ceil((r.height - 30) / (Grid.TOTAL_SEQUENCES + 1f)) + 1;

        current = new CurrentBeat();

        ArrayList<BeatLabel> lbls = current.createLabels();
        for (int i = 0; i < lbls.size(); i++) {
            BeatLabel lbl = lbls.get(i);
            lbl.setBounds(i * colWidth + 3, 1, 26, 26);
            add(lbl);
        }
        addMouseListener(this);
    }

    private Point translate(Point p) {
        if (p.y < rowHeight + 2) return new Point(p.x / colWidth, -1);
        return new Point(p.x / colWidth,  (p.y - rowHeight) / rowHeight);
    }

    Color color;
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;

        Grid grid = BeatsView.getCurrent();
        for (int x = 0; x < JudahClock.getSteps(); x++) {
            color = x % JudahClock.getSubdivision() == 0 ? Pastels.BLUE :Color.WHITE;
            // g2d.setPaint(color);

            for (int y = 0; y < grid.size(); y++) {
                if (grid.get(y).hasStep(x))
                    g2d.setPaint(Pastels.forType(grid.get(y).getStep(x).getType()));
                else
                    g2d.setPaint(color);
                g2d.fillOval(x * colWidth + 3, y * rowHeight + rowHeight + 5, ROW_HEIGHT, ROW_HEIGHT);
            }
        }
    }

    @Override public void mouseClicked(MouseEvent e) {
        Point xy = translate(e.getPoint());
        if (xy.y < 0) { // label row clicked, user wants to hear this step
            if (xy.x >= 0 && xy.x< JudahClock.getSteps())
                if (SwingUtilities.isRightMouseButton(e))
                    for (BeatBox beatbox : JudahClock.getInstance().getSequencers())
                        beatbox.step(xy.x);
                else
                    BeatsView.getSequencer().step(xy.x);
            return;
        }
        if (xy.x >= JudahClock.getSteps() || xy.x < 0) return; // off grid
        if (xy.y >= BeatsView.getCurrent().size()) return; // off grid
        
        Sequence beats = BeatsView.getCurrent().get((xy.y));
        Beat b = beats.getStep(xy.x);
        if (b == null) {
            Beat created = new Beat(xy.x);
            beats.add(created);
            processNoteOff(beats, created);
        }
        else
            beats.remove(b);
        repaint();
    }

    private void processNoteOff(Sequence seq, Beat created) {
        if (BeatBox.Type.Drums == BeatsView.getSequencer().getType())
            return;
        int gate = BeatsView.getInstance().getNoteOff();
        if (gate == 0) return; // no note off
        int next = gate + created.getStep();
        if (next >= JudahClock.getSteps())
            next -= JudahClock.getSteps();
        if (!seq.hasStep(next))
            seq.add(new Beat(next, Type.NoteOff));
    }

    @Override public void mousePressed(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }
}
