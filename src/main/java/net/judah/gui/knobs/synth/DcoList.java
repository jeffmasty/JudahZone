package net.judah.gui.knobs.synth;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import judahzone.gui.Gui;
import judahzone.synth.Dco.DcoJson;
import judahzone.synth.WavSynth;
import net.judah.gui.Size;

public class DcoList extends JPanel {

    private final WavSynth synth;
    /** Stable list of DcoEdit references, kept in sync with algo order. */
    private final List<DcoEdit> bars = new ArrayList<>();

    public DcoList(WavSynth synth) {
        this.synth = synth;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { if (e.isPopupTrigger()) showMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
            @Override public void mouseClicked(MouseEvent e)  { if (e.isPopupTrigger()) showMenu(e); }

            private void showMenu(MouseEvent e) {
                Component src = (Component) e.getSource();
                Point p = SwingUtilities.convertPoint(src, e.getPoint(), DcoList.this);
                Component hit = getComponentAt(p);
                int idx = -1;
                for (int i = 0; i < bars.size(); i++) {
                    if (hit == bars.get(i) || SwingUtilities.isDescendingFrom(hit, bars.get(i))) {
                        idx = i;
                        break;
                    }
                }
                new DcoMenu(synth, DcoList.this, idx).show(p.x, p.y);
            }
        });
    }

    private static final Dimension BAR = new Dimension(Size.WIDTH_KNOBS - 30, Size.STD_HEIGHT + 6);

    public void load(DcoJson[] algo) {
        SwingUtilities.invokeLater(() -> {
            removeAll();
            bars.clear();
            for (int i = 0; i < algo.length; i++) {
                DcoEdit edit = new DcoEdit(synth, i);
                bars.add(edit);
                add(Gui.resize(edit, BAR));
            }
            revalidate();
            repaint();
        });
    }

    /** @return number of DcoEdit bars currently shown (use instead of getComponentCount()) */
    public int barCount() {
        return bars.size();
    }

    /** Update a specific DcoEdit at index i with new DcoJson data. */
    public void updateBar(int i, DcoJson dco) {
        if (i >= 0 && i < bars.size()) {
            bars.get(i).update(dco);
        }
    }

}
