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

    @Getter private final CurrentBeat current;

    private final JudahClock clock = JudahClock.getInstance();
    private final int row;
    private final int col;

    public GridView(Rectangle r) {
        setOpaque(false);
        setLayout(null);

        col = r.width  / clock.getSteps();
        row = (int)Math.ceil((r.height - 30) / (Grid.TOTAL_SEQUENCES + 1f)) + 1;

        current = new CurrentBeat();

        ArrayList<BeatLabel> lbls = current.createLabels();
        for (int i = 0; i < lbls.size(); i++) {
            BeatLabel lbl = lbls.get(i);
            lbl.setBounds(i * col + 3, 1, 26, 26);
            add(lbl);
        }
        addMouseListener(this);
    }

    private Point translate(Point p) {
        if (p.y < row + 2) return new Point(p.x / col, -1);
        return new Point(p.x / col,  (p.y - row) / row);
    }

    Color color;
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;

        Grid grid = BeatsView.getInstance().getCurrent();
        for (int x = 0; x < clock.getSteps(); x++) {
            color = x % clock.getSubdivision() == 0 ? Pastels.BLUE :Color.WHITE;
            // g2d.setPaint(color);

            for (int y = 0; y < grid.size(); y++) {
                if (grid.get(y).hasStep(x))
                    g2d.setPaint(Pastels.forType(grid.get(y).getStep(x).getType()));
                else
                    g2d.setPaint(color);
                g2d.fillOval(x * col + 3, y * row + row + 5, 24, 24);
            }
        }
    }

    @Override public void mouseClicked(MouseEvent e) {
        Point xy = translate(e.getPoint());
        if (xy.y < 0) { // label row clicked, user wants to hear this step
            if (xy.x >= 0 && xy.x< clock.getSteps())
                if (SwingUtilities.isRightMouseButton(e))
                    for (BeatBox beatbox : JudahClock.getInstance().getSequencers())
                        beatbox.process(xy.x);
                else
                    BeatsView.getInstance().getSequencer().process(xy.x);
            return;
        }
        if (xy.x >= clock.getSteps() || xy.x < 0) return; // off grid
        Sequence beats = BeatsView.getInstance().getCurrent().get((xy.y));
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
        if (BeatBox.Type.Drums == BeatsView.getInstance().getSequencer().getType())
            return;
        int gate = BeatsView.getInstance().getNoteOff();
        if (gate == 0) return; // no note off
        int next = gate + created.getStep();
        if (next >= clock.getSteps())
            next -= clock.getSteps();
        if (!seq.hasStep(next))
            seq.add(new Beat(next, Type.NoteOff));
    }

    @Override public void mousePressed(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }
}
