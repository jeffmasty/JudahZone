package net.judah;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import lombok.Getter;
import net.judah.clock.JudahClock;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPK;
import net.judah.effects.gui.EffectsRack;
import net.judah.effects.gui.PresetsGui;
import net.judah.looper.Recorder;
import net.judah.looper.Sample;
import net.judah.metronome.MidiGnome;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.song.SonglistTab;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

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

        for (Sample loop : JudahZone.getLooper().getLoops()) 
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

        //add(new JLabel(" ")); // filler
        add(tabs);
        add(metronome(new String[] {"JudahZone.mid", "44_Minor_4-4_i_-III_iv_V.mid", "BoogieWoogie.mid"}));

        //add(new JLabel(" ")); // filler
        add(console);        

        doLayout();
    }

    private JPanel metronome(String[] files) {
    	JPanel result = new JPanel();
    	for (String file : files) {
			try {
				MidiGnome playa = new MidiGnome(new File("metronome/" + file));
				JButton action = new JButton(file.substring(0, 12));
				action.setFont(Constants.Gui.FONT10);
				result.add(action);
				action.addActionListener(e -> {
					if (playa.isRunning())
						playa.stop();
					else
						try {
							playa.start();
						} catch (MidiUnavailableException e1) {
							RTLogger.warn(ControlPanel.this, e1);
						}
				});
			
			
			} catch (MidiUnavailableException e) {
				RTLogger.warn(this, e);
			}
    	}
    	return result;
    }
    
    public void setFocus(Channel ch) {
    	MPK.setMode(KnobMode.Effects1);

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
        MainFrame.updateCurrent();
    }

    public Channel getChannel() {
        if (current == null) return null;
        return current.getChannel();
    }

	public void beatBuddy() {
		tabs.setSelectedComponent(JudahClock.getInstance().getDrummachine().getGui());
	}


}
