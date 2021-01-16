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
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.mixer.ChannelGui;
import net.judah.mixer.LineIn;
import net.judah.mixer.SoloTrack;
import net.judah.song.SonglistTab;
import net.judah.util.Constants;

public class MixerPane extends JPanel {

    //public static final int WIDTH = 360;

    @Getter private static MixerPane instance;

    private JComponent songlist;
    @Getter private final JTabbedPane tabs;
    private final JPanel mixer = new JPanel();
    private final LooperGui looper;
    @Getter private final SoloTrack highlight = new SoloTrack();

    public MixerPane() {

        instance = this;
        setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

        tabs = new JTabbedPane();
        songlist = new SonglistTab(Constants.defaultSetlist);
        tabs.add("Setlist", songlist);
        // tabs.add("Metronome", JudahZone.getMetronome().getGui());

        tabs.add("BeatBuddy", JudahZone.getDrummachine().getGui());

        tabs.add("Channel", highlight);
        tabs.setSelectedIndex(tabs.getComponentCount() - 1);
        add(tabs);

        mixer.setLayout(new GridLayout(0,2));
        mixer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(), // BorderFactory.createLineBorder(Color.DARK_GRAY),
                "Input", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, Constants.Gui.FONT11));

        looper = new LooperGui(JudahZone.getLooper());
        add(looper);
        add(mixer);
        for (LineIn channel : JudahZone.getChannels())
            mixer.add(channel.getGui());

        setFocus(JudahZone.getMasterTrack());
        JudahZone.getLooper().getDrumTrack().getGui().doLayout();

    }

    public void update() {
        for (Channel c : JudahZone.getChannels()) c.getGui().update();
        for (Sample s : JudahZone.getLooper()) s.getGui().update();
        JudahZone.getLooper().getDrumTrack().getGui().update();
        JudahZone.getMasterTrack().getGui().update();
        SoloTrack.getInstance().update();
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
