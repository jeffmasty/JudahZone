package net.judah;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.mixer.ChannelGui;
import net.judah.util.Console;
import net.judah.util.Constants;

public class LooperGui extends JPanel {

    public LooperGui(Looper looper) {
        setLayout(new GridLayout(0, 2));
        setBorder(BorderFactory.createTitledBorder(
                // BorderFactory.createEmptyBorder(),
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                "Output", TitledBorder.CENTER, TitledBorder.BELOW_TOP, Constants.Gui.FONT11));
        add(JudahZone.getMasterTrack().getGui());
        add(looper.getDrumTrack().getGui());

        for (Sample loop : JudahZone.getLooper())
            add(loop.getGui());
        looper.registerListener(this);
    }

    public void addSample(Sample s) {
        Console.info("Add Sample gui " + s.getName());
        add(s.getGui());
        for (Component c : getComponents())
            c.doLayout();
        MixerPane.getInstance().doLayout();
        Color bg = getBackground();
        Color next = new Color(bg.getRGB() + 1);
        setBackground(next);
    }

    public void removeSample(Sample s) {
        remove(s.getGui());
        doLayout();
    }

    public void setSelected(Channel bus) {
        for (Component c : getComponents())
            if (c instanceof ChannelGui.Output)
                ((ChannelGui.Output)c).getLabelButton().setSelected(bus == ((ChannelGui.Output)c).getChannel());
    }

}

