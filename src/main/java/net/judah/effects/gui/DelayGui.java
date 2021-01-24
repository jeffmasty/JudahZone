package net.judah.effects.gui;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.judah.effects.Delay;
import net.judah.mixer.Channel;
import net.judah.util.Console;
import net.judah.util.Constants.Gui;
import net.judah.util.TapTempo;

public class DelayGui extends Widget {

    private Slider delFeedback, delTime;

    public DelayGui(Channel ch) {
        super(ch, new JToggleButton(" Delay "), e -> {
            ch.getDelay().setActive(!ch.getDelay().isActive());});
        delFeedback = new Slider(0, 100, e -> {
            delFeedback(delFeedback.getValue());
        });
        delFeedback.setMaximumSize(Gui.SLIDER_SZ);
        delFeedback.setPreferredSize(Gui.SLIDER_SZ);

        delTime = new Slider(0, 100, e -> {
            delTime(delTime.getValue());
        });
        delTime.setMaximumSize(Gui.SLIDER_SZ);
        delTime.setPreferredSize(Gui.SLIDER_SZ);

        TapTempo tapButton = new TapTempo(" time/sync ", msec -> {
            delayTapTempo(msec);
        });
        tapButton.setPreferredSize(TAP_SZ);
        tapButton.setMaximumSize(TAP_SZ);

        add(delTime);
        add(tapButton);
        add(labelPanel("feedback", delFeedback));
        add(Box.createRigidArea(new Dimension(15, 1)));

    }

    @Override
    void update() {
        Delay delay = channel.getDelay();
        activeButton.setSelected(delay.isActive());
        delFeedback.setValue(Math.round(delay.getFeedback() * 100));
        delTime.setValue(delTime());
    }

    private int delTime() {
        float max = channel.getDelay().getMaxDelay();
        // result / 100 = delay / max
        return Math.round(100 * channel.getDelay().getDelay() / max);
    }

    private void delTime(int val) {
        float max = channel.getDelay().getMaxDelay();
        // val/100 = set()/max
        channel.getDelay().setDelay(val * max / 100f);
    }

    private void delFeedback(int val) {
        channel.getDelay().setFeedback(val / 100f);
    }

    private void delayTapTempo(long msec) {
        if (msec > 0) {
            float old = channel.getDelay().getDelay();
            channel.getDelay().setDelay(msec / 1000f);
            Console.info("from " + old + " to " + channel.getDelay().getDelay() + " delay.");
            delTime.setValue(delTime());
        } else {
            Console.info("right clicked");
        }
    }

    private JPanel labelPanel(String name, Component c) {
        JPanel pnl = new JPanel();
        pnl.add(c);
        JLabel lbl = new JLabel(name);
        lbl.setFont(Gui.FONT11);
        lbl.setPreferredSize(TAP_SZ);
        lbl.setMaximumSize(TAP_SZ);
        pnl.add(lbl);
        return pnl;
    }

}
