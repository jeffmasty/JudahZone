package net.judah;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

import lombok.Getter;
import net.judah.effects.gui.EffectsRack;
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.mixer.ChannelGui;
import net.judah.mixer.LineIn;
import net.judah.song.SonglistTab;
import net.judah.util.Constants;

public class MixerPane extends JPanel {

    @Getter private static MixerPane instance;

    private JComponent songlist;
    @Getter private final JTabbedPane tabs;
    private final JPanel mixer = new JPanel();
    private final LooperGui looper;
    @Getter private final EffectsRack highlight = new EffectsRack();

    public MixerPane() {

        instance = this;
        setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

        looper = new LooperGui(JudahZone.getLooper());
        mixer.setLayout(new GridLayout(0,2));
        mixer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(), // BorderFactory.createLineBorder(Color.DARK_GRAY),
                "Input", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, Constants.Gui.FONT11));
        for (LineIn channel : JudahZone.getChannels())
            mixer.add(channel.getGui());

        tabs = new JTabbedPane();
        songlist = new SonglistTab(Constants.defaultSetlist);
        tabs.add("Setlist", songlist);

        tabs.add("BeatBuddy", JudahZone.getDrummachine().getGui());

        // tabs.add("Presets", new PresetsGui());

        tabs.add("Channel", highlight);
        tabs.setSelectedIndex(tabs.getComponentCount() - 1);

        add(looper);
        add(mixer);
        add(tabs);
    }

    public void update() {
        for (Channel c : JudahZone.getChannels()) c.getGui().update();
        for (Sample s : JudahZone.getLooper()) s.getGui().update();

        JudahZone.getMasterTrack().getGui().update();
        EffectsRack.getInstance().update();
    }

    public void setFocus(Channel bus) {
        highlight.setFocus(bus);
        tabs.setSelectedComponent(highlight);
        tabs.setTitleAt(tabs.indexOfComponent(highlight), bus.getName());
        bus.getGui().getLabelButton().requestFocus();
        for(Component c : mixer.getComponents()) {
            if (c instanceof ChannelGui.Input) {
                ChannelGui.Input gui = (ChannelGui.Input)c;
                gui.getLabelButton().setSelected(gui.getChannel() == bus);
            }
        }
        looper.setSelected(bus);
    }

}
