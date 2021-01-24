package net.judah.effects.gui;

import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.judah.JudahZone;
import net.judah.effects.CutFilter;
import net.judah.effects.CutFilter.Type;
import net.judah.mixer.Channel;
import net.judah.util.Console;
import net.judah.util.Constants.Gui;

public class CutFilterGui extends JPanel implements GUI {

    public static final String EQ_PARTY = "pArTy";
    public static final String EQ_LOCUT = "LoCut";
    public static final String EQ_HICUT = "HiCut";
    public static final String[] EQs = new String[]
            { EQ_LOCUT, EQ_PARTY, EQ_HICUT };

    protected final Channel channel;
    private JToggleButton activeButton;
    private Slider cutFreq, cutRes;
    private JComboBox<String> cutType;

    public CutFilterGui(Channel ch) {
        this.channel = ch;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(Gui.GRAY1);

        activeButton = new JToggleButton("CutEQ");
        activeButton.addActionListener(listener -> {
            channel.getCutFilter().setActive(!channel.getCutFilter().isActive());
            update();
            Console.info(channel.getName() + " CUT: " + (channel.getCutFilter().isActive() ? " On" : " Off"));
        });
        DefaultComboBoxModel<String> cutModel = new DefaultComboBoxModel<>(EQs);
        cutType = new JComboBox<>(cutModel);
        cutType.addActionListener(e -> {cutFilterType();});
        cutFreq = new Slider(0, 100, e -> { if (!JudahZone.isInitialized()) return;
            channel.getCutFilter().setFrequency(CutFilter.knobToFrequency(cutFreq.getValue()));
        });
        cutFreq.setMaximumSize(Gui.SLIDER_SZ);
        cutFreq.setPreferredSize(Gui.SLIDER_SZ);
        cutRes = new Slider(0, 100,
                l -> {channel.getCutFilter().setResonance(cutRes.getValue() * 0.25f);}, "Resonance Db");
        cutRes.setPreferredSize(Gui.SLIDER_SZ);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        row1.add(activeButton);
        row1.add(cutFreq);
        JLabel hz = new JLabel("hz.");
        hz.setFont(Gui.FONT11);
        row1.add(hz);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        row2.add(cutType);
        row2.add(cutRes);
        JLabel res = new JLabel("res");
        res.setFont(Gui.FONT11);
        row2.add(res);
        add(row1);
        add(row2);

    }

    public void update() {
        CutFilter cutFilter = channel.getCutFilter();
        activeButton.setSelected(cutFilter.isActive());
        cutType.setSelectedItem(cutFilter.getFilterType().name());

        float freq = cutFilter.getFrequency();
        int knob = CutFilter.frequencyToKnob(freq);
        cutFreq.setValue( knob );
        cutRes.setValue((int)(cutFilter.getResonance() * 4));

    }
    private void cutFilterType() {
        CutFilter cutFilter = channel.getCutFilter();
        switch(cutType.getSelectedItem().toString()) {
        case EQ_PARTY: cutFilter.setFilterType(Type.pArTy); break;
        case EQ_HICUT: cutFilter.setFilterType(Type.LP12); break;
        case EQ_LOCUT: cutFilter.setFilterType(Type.HP12); break;
        }
    }

}
