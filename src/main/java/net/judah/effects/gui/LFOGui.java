package net.judah.effects.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.*;

import net.judah.JudahZone;
import net.judah.effects.LFO;
import net.judah.effects.LFO.Target;
import net.judah.mixer.Channel;
import net.judah.util.Constants;
import net.judah.util.Constants.Gui;
import net.judah.util.Knob;
import net.judah.util.RTLogger;
import net.judah.util.Slider;
import net.judah.util.TapTempo;

// loop opitons

public class LFOGui extends JPanel implements GUI {

    protected final Channel channel;
    private JToggleButton activeButton;
    private Slider lfoFreq;
    private JComboBox<String> lfoTarget;
    private Knob lfoMin, lfoMax;
    private final PresetCombo presets;


    public LFOGui(Channel ch) {
        this.channel = ch;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(Gui.GRAY1);

        presets = new PresetCombo(channel, JudahZone.getPresets());
        activeButton = new JToggleButton("  LFO   ");
        activeButton.addActionListener(e -> {lfo();});
        lfoMax = new Knob(val -> {channel.getLfo().setMax(val);});
        lfoMax.setValue(90);
        lfoMin = new Knob(val -> {channel.getLfo().setMin(val);});
        lfoMin.setValue(10);
        
        lfoFreq = new Slider(90, 1990, e -> {channel.getLfo().setFrequency(
                lfoFreq.getValue());}, "0.1 to 2 seconds");
        lfoFreq.setMaximumSize(Gui.SLIDER_SZ);
        lfoFreq.setPreferredSize(Gui.SLIDER_SZ);

        DefaultComboBoxModel<String> lfoModel = new DefaultComboBoxModel<>();
        for (Target t : LFO.Target.values())
            lfoModel.addElement(t.name());
        lfoTarget = new JComboBox<>(lfoModel);
        lfoTarget.setSelectedItem(channel.getLfo().getTarget().toString());
        lfoTarget.addActionListener(e -> { channel.getLfo().setTarget(
                Target.valueOf(lfoTarget.getSelectedItem().toString()));});
        lfoTarget.setPreferredSize(TARGET_SZ);
        lfoTarget.setMaximumSize(TARGET_SZ);

        JPanel row1 = new JPanel();
        row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS)); 
        row1.add(activeButton);
        JLabel minmax = new JLabel("<html><u>min</u><br/>max</html>", JLabel.CENTER);
        minmax.setFont(Gui.FONT10);

        row1.add(Constants.wrap(lfoMin, minmax, lfoMax));
        row1.add(presets);
        
        row1.add(Box.createHorizontalGlue());

//        row1.add(Box.createHorizontalGlue());
//        row1.add(lfoFreq);
        TapTempo time = new TapTempo("time", msec -> {
            if (msec > 0) {
                channel.getLfo().setFrequency((int)msec);
                lfoFreq.setValue((int)channel.getLfo().getFrequency());
                RTLogger.log(this, "LFO Tap Tempo: " + channel.getLfo().getFrequency());
            }
        });
        Dimension TAP_SZ = new Dimension(50, 22);
        time.setPreferredSize(TAP_SZ);
        time.setMaximumSize(TAP_SZ);
        // row1.add(time);
        // row1.add(Box.createRigidArea(SPACER));

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));

        row2.setLayout(new BoxLayout(row2, BoxLayout.X_AXIS)); // new FlowLayout(FlowLayout.CENTER, 3, 0));
        row2.add(Box.createRigidArea(SPACER));
        row2.add(lfoTarget);
        row2.add(time);
        row2.add(lfoFreq);
        row2.add(Box.createHorizontalGlue());
        
//        row2.add(Box.createHorizontalGlue());
//        row2.add(Box.createRigidArea(SPACER));

        add(row1);
        add(row2);

    }

    public void update() {
        LFO lfo = channel.getLfo();
        activeButton.setSelected(lfo.isActive());
        lfoMax.setValue(lfo.getMax());
        lfoMin.setValue(lfo.getMin());
        lfoFreq.setValue((int)lfo.getFrequency());
        lfoTarget.setSelectedIndex(lfo.getTarget().ordinal());
        if (channel.getPreset() != null)
        	presets.setSelectedItem(channel.getPreset());

    }

    public void lfo() {
        LFO lfo = channel.getLfo();
        lfo.setActive(!lfo.isActive());
        if (lfo.getTarget() == Target.CutEQ)
            channel.getCutFilter().setActive(lfo.isActive());
        // if (lfo.isActive()) lfoRecover = ch.getVolume(); else ch.setVolume(lfoRecover);
        RTLogger.log(this, channel.getName() + " LFO: " + (lfo.isActive() ? " On" : " Off"));
    }

    public void knobs(int idx, int data2) {
//    	Preset preset = (Preset)Constants.ratio(data2, JudahZone.getPresets());
//    	channel.setPreset(preset);
    }
    

}
