package net.judah;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import lombok.Getter;
import net.judah.clock.JudahClock;
import net.judah.effects.gui.EffectsRack;
import net.judah.effects.gui.PresetsGui;
import net.judah.looper.Recorder;
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.song.SonglistTab;
import net.judah.util.Console;
import net.judah.util.Constants;

public class ControlPanel extends JPanel {

    @Getter private static ControlPanel instance;
    @Getter private EffectsRack current;
    // proper class??
    @Getter private static Recorder liveLoop = JudahZone.getLooper().getLoopA();  
    @Getter private static LineIn liveInput = JudahZone.getChannels().getGuitar();
    private JComponent songlist;
    private final JTabbedPane tabs;
    
    private class EffectsList extends ArrayList<EffectsRack> {
        EffectsRack get(Channel ch) {
            for (EffectsRack r : this)
                if (r.getChannel().equals(ch))
                    return r;
            throw new InvalidParameterException("" + ch);
        }
    }
    private final EffectsList effectsTab = new EffectsList();
    
    public ControlPanel() {
        instance = this;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        effectsTab.add(new EffectsRack(JudahZone.getMasterTrack()));

        for (Sample loop : JudahZone.getLooper()) 
            effectsTab.add(new EffectsRack(loop));
        for (LineIn input : JudahZone.getChannels()) 
        	effectsTab.add(new EffectsRack(input));
        // register listener?

        tabs = new JTabbedPane();
        songlist = new SonglistTab(Constants.defaultSetlist);
        tabs.add("Setlist", songlist);
        tabs.add("BeatBuddy", JudahClock.getInstance().getDrummachine().getGui());
        tabs.add("Presets", new PresetsGui(JudahZone.getPresets()));
        
        JPanel console = new JPanel();
        console.setLayout(new BoxLayout(console, BoxLayout.Y_AXIS));
        JTextField input = Console.getInstance().getInput();
        JScrollPane output = Console.getInstance().getScroller();
        console.add(output);
        console.add(input);

        add(JudahClock.getInstance().getGui());
        add(new JLabel(" ")); // filler
        add(tabs);
        add(new JLabel(" ")); // filler
        add(console);        

        doLayout();
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
        if (ch instanceof Recorder) 
        	liveLoop = (Recorder)ch;
        else if (ch instanceof LineIn)
        	liveInput = (LineIn)ch;

        if (idx < 0) {
            tabs.add(EffectsRack.TAB_NAME, current);
        }
        else {
            tabs.setComponentAt(idx, current);
            current.update();
        }
        tabs.setSelectedComponent(current);
    }

    public Channel getChannel() {
        if (current == null) return null;
        return current.getChannel();
    }


}
