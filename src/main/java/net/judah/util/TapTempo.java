package net.judah.util;

import java.awt.SystemColor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.judah.util.Constants.Gui;

/**A JLabel that establishes a tap tempo on mouse clicks */
public class TapTempo extends JLabel {

    private long last;

    public static interface TapDancer {
        /**Called when a tap tempo has been established or on right click (msec = -1)
         * @param msec the most recent tap tempo or -1 if right mouse button clicked */
        void taptap(long msec);
    }

    public TapTempo(String lbl) {
        super(lbl, JLabel.CENTER);
        setFont(Gui.FONT11);
        setBorder(BorderFactory.createDashedBorder(SystemColor.inactiveCaption));
    }

    public TapTempo(String lbl, TapDancer listener) {
        this(lbl);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    listener.taptap(-1);
                    return;
                }
                long current = System.currentTimeMillis();
                if ((current - last) > 3000) {
                    // reset, not going to establish more than 3 second tap tempo
                    last = current;
                    return;
                }
                listener.taptap(current - last);
                last = current;
            }
        });
    }

}
