package net.judah;

import java.awt.Dimension;
import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import lombok.Getter;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPK;
import net.judah.effects.gui.EffectsRack;
import net.judah.looper.Loop;
import net.judah.metronome.MidiGnome;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.util.*;

public class ControlPanel extends JPanel {

    @Getter private static ControlPanel instance;
    @Getter private EffectsRack current;
    @Getter private final GuitarTuner tuner = new GuitarTuner();
    private JPanel placeholder = new JPanel();
    
    //private JComponent songlist;
    //private final JTabbedPane tabs;
    
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

        for (Loop loop : JudahZone.getLooper().getLoops()) 
            effectsTab.add(new EffectsRack(loop));
        for (LineIn input : JudahZone.getChannels()) 
        	effectsTab.add(new EffectsRack(input));

        placeholder.addKeyListener(JudahMenu.getInstance());
        placeholder.setFocusTraversalKeysEnabled(false);
        
        JPanel console = new JPanel();
        console.setLayout(new BoxLayout(console, BoxLayout.Y_AXIS));
        JTextField input = Console.getInstance().getInput();
        JScrollPane output = Console.getInstance().getScroller();

        Dimension d = new Dimension(Size.WIDTH_CONTROLS - 10, 25);
        input.setPreferredSize(d);
        input.setMaximumSize(d);
        input.setMinimumSize(d);
        d = new Dimension(Size.WIDTH_CONTROLS - 10, 90);
        output.setPreferredSize(d);
        output.setMaximumSize(d);

        
        console.add(output);
        console.add(input);

        add(tuner);

        add(placeholder);
        placeholder.add(effectsTab.get(0));
        

        add(console);        

        doLayout();
    }

    @SuppressWarnings("unused")
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
    		return;
    	}
    	new Thread(()->{
	        current = effectsTab.get(ch);
	        placeholder.removeAll();
	        placeholder.add(current);
	        placeholder.requestFocus();
	        validate();
	        MainFrame.updateCurrent();
    
    	}).start();

    }

    public Channel getChannel() {
        if (current == null) return null;
        return current.getChannel();
    }


}
