package net.judah;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

import lombok.Getter;
import net.judah.effects.gui.EffectsRack;
import net.judah.effects.gui.PresetsGui;
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.mixer.ChannelGui;
import net.judah.mixer.LineIn;
import net.judah.song.SonglistTab;
import net.judah.util.Constants;

public class MixerPane extends JPanel {

    @Getter private static MixerPane instance;

    private final EffectsList effectsTab = new EffectsList();

    @Getter private EffectsRack current;
    private JComponent songlist;
    private final JTabbedPane tabs;
    private final JPanel mixer = new JPanel();
    private final LooperGui looper;

    public MixerPane() {

        instance = this;
        setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

        looper = new LooperGui(JudahZone.getLooper(), effectsTab);

        mixer.setLayout(new GridLayout(0,2));
        mixer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(), // BorderFactory.createLineBorder(Color.DARK_GRAY),
                "Mixer", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, Constants.Gui.FONT11));
        for (LineIn channel : JudahZone.getChannels()) {
            mixer.add(channel.getGui());
            effectsTab.add(new EffectsRack(channel));
        }

        tabs = new JTabbedPane();
        songlist = new SonglistTab(Constants.defaultSetlist);
        tabs.add("Setlist", songlist);
        tabs.add("BeatBuddy", JudahZone.getDrummachine().getGui());
        tabs.add("Presets", new PresetsGui(JudahZone.getPresets()));
        add(looper);
        add(mixer);
        add(tabs);
        doLayout();
    }

    public void update() {
        for (Channel c : JudahZone.getChannels()) c.getGui().update();
        for (Sample s : JudahZone.getLooper()) s.getGui().update();
        JudahZone.getMasterTrack().getGui().update();
        if (current != null) current.update();
    }

    public void setFocus(Channel ch) {
        if (ch.equals(getChannel())) {
            tabs.setSelectedComponent(current);
            return;
        }
        int idx = -1;
        for (int i = 0; i < tabs.getComponentCount(); i++) {
            if (tabs.getTitleAt(i).equals(EffectsRack.TAB_NAME)) {
                idx = i;
                break;
            }
        }

        current = effectsTab.get(ch);

        if (idx < 0) {
            tabs.add(EffectsRack.TAB_NAME, current);
        }
        else {
            tabs.setComponentAt(idx, current);
            current.update();
        }

        tabs.setSelectedComponent(current);
        for(Component c : mixer.getComponents()) {
            if (c instanceof ChannelGui.Input) {
                ChannelGui.Input gui = (ChannelGui.Input)c;
                gui.getLabelButton().setSelected(gui.getChannel() == ch);
            }
        }
        looper.setSelected(ch);
        JudahZone.getMasterTrack().getGui().getLabelButton().setSelected(
                ch == JudahZone.getMasterTrack());
        ch.getGui().getLabelButton().requestFocus();
    }

    public Channel getChannel() {
        if (current == null) return null;
        return current.getChannel();
    }

    public EffectsRack getEffects() {
        return current;
    }

    // TODO overdrive?
    public static void volume(Channel ch) {
        if (getInstance() == null || getInstance().getChannel() == null) return;
        if (!getInstance().getChannel().equals(ch)) return;
        getInstance().getEffects().getVolume().setValue(ch.getVolume());
    }

    class EffectsList extends ArrayList<EffectsRack> {
        EffectsRack get(Channel ch) {
            for (EffectsRack r : this)
                if (r.getChannel().equals(ch))
                    return r;
            throw new InvalidParameterException("" + ch);
        }
    }


}
