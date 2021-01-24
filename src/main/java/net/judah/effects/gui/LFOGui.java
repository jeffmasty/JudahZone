package net.judah.effects.gui;

import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.judah.effects.LFO;
import net.judah.effects.LFO.Target;
import net.judah.mixer.Channel;
import net.judah.util.Console;
import net.judah.util.Constants.Gui;
import net.judah.util.Knob;
import net.judah.util.TapTempo;

public class LFOGui extends JPanel implements GUI {

    protected final Channel channel;
    private JToggleButton activeButton;
    private Slider lfoFreq;
    private JComboBox<String> lfoTarget;
    private Knob lfoMin, lfoMax;

    public LFOGui(Channel ch) {
        this.channel = ch;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(Gui.GRAY1);

        activeButton = new JToggleButton("  LFO   ");
        activeButton.addActionListener(e -> {lfo();});
        lfoMax = new Knob(val -> {channel.getLfo().setMax(val);});
        lfoMin = new Knob(val -> {channel.getLfo().setMin(val);});

        lfoFreq = new Slider(90, 1990, e -> {channel.getLfo().setFrequency(
                lfoFreq.getValue());}, "0.1 to 2 seconds");
        lfoFreq.setMaximumSize(Gui.SLIDER_SZ);
        lfoFreq.setPreferredSize(Gui.SLIDER_SZ);
        DefaultComboBoxModel<String> lfoModel = new DefaultComboBoxModel<>();
        for (Target t : LFO.Target.values())
            lfoModel.addElement(t.name());
        lfoTarget = new JComboBox<>(lfoModel);
        lfoTarget.addActionListener(e -> { channel.getLfo().setTarget(
                Target.valueOf(lfoTarget.getSelectedItem().toString()));});
        lfoTarget.setPreferredSize(TARGET_SZ);
        lfoTarget.setMaximumSize(TARGET_SZ);

        JPanel row1 = new JPanel();
        row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS)); // new FlowLayout(FlowLayout.CENTER, 3, 0));
        row1.add(Box.createRigidArea(SPACER));
        row1.add(activeButton);
        row1.add(Box.createHorizontalGlue());
        row1.add(lfoFreq);
        TapTempo time = new TapTempo(" time/sync ", msec -> {
            if (msec > 0) {
                channel.getLfo().setFrequency(msec);
                lfoFreq.setValue((int)channel.getLfo().getFrequency());
                Console.info("LFO Tap Tempo: " + channel.getLfo().getFrequency());
            }
        });
        time.setPreferredSize(TAP_SZ);
        time.setMaximumSize(TAP_SZ);
        row1.add(time);
        row1.add(Box.createRigidArea(SPACER));

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));

        row2.setLayout(new BoxLayout(row2, BoxLayout.X_AXIS)); // new FlowLayout(FlowLayout.CENTER, 3, 0));
        row2.add(Box.createRigidArea(SPACER));
        row2.add(lfoTarget);
        row2.add(Box.createHorizontalGlue());
        JLabel min = new JLabel("min");
        min.setFont(Gui.FONT11);
        row2.add(min);
        row2.add(lfoMin);

        JLabel max = new JLabel("max");
        max.setFont(Gui.FONT11);
        row2.add(max);
        row2.add(lfoMax);
        row2.add(Box.createRigidArea(SPACER));

        add(row1);
        add(row2);

    }

    public void update() {
        LFO lfo = channel.getLfo();
        activeButton.setSelected(lfo.isActive());
        lfoMax.setValue((int)lfo.getMax());
        lfoMin.setValue((int)lfo.getMin());
        lfoFreq.setValue((int)lfo.getFrequency());
        lfoTarget.setSelectedIndex(lfo.getTarget().ordinal());
    }

    public void lfo() {
        LFO lfo = channel.getLfo();
        lfo.setActive(!lfo.isActive());
        if (lfo.getTarget() == Target.CutEQ)
            channel.getCutFilter().setActive(lfo.isActive());
        // if (lfo.isActive()) lfoRecover = ch.getVolume(); else ch.setVolume(lfoRecover);
        Console.info(channel.getName() + " LFO: " + (lfo.isActive() ? " On" : " Off"));
    }


}
