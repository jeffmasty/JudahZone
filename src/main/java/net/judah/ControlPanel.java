package net.judah;

import java.awt.Dimension;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import lombok.Getter;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPK;
import net.judah.effects.gui.EffectsRack;
import net.judah.looper.Loop;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.util.*;

public class ControlPanel extends JPanel {

    @Getter private static ControlPanel instance;
    @Getter private EffectsRack current;
    @Getter private final GuitarTuner tuner = new GuitarTuner();
    private JPanel placeholder = new JPanel();
    
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
        setBorder(Constants.Gui.NONE);
        setBackground(Pastels.EGGSHELL);
        current = new EffectsRack(JudahZone.getMasterTrack());
        effectsTab.add(current);
        for (Loop loop : JudahZone.getLooper().getLoops()) 
            effectsTab.add(new EffectsRack(loop));
        for (LineIn input : JudahZone.getChannels()) {
        	EffectsRack rack = new EffectsRack(input);
        	effectsTab.add(rack);
        }
        	
        placeholder.addKeyListener(JudahMenu.getInstance());
        placeholder.setFocusTraversalKeysEnabled(false);
        add(tuner);
        add(placeholder);
        placeholder.add(effectsTab.get(0));

        JPanel console = new JPanel();
        console.setLayout(new BoxLayout(console, BoxLayout.Y_AXIS));
        JScrollPane output = Console.getInstance().getScroller();
        Dimension d = new Dimension(Size.WIDTH_CONTROLS, 90);
        output.setPreferredSize(d);
        output.setMaximumSize(d);
        console.add(output);
        // JTextField input = Console.getInstance().getInput();
        //d = new Dimension(Size.WIDTH_CONTROLS - 10, 25);
		//input.setPreferredSize(d);
		//input.setMaximumSize(d);
		//input.setMinimumSize(d);
        // console.add(input);
        add(console);        

        doLayout();
    }

    void setFocus(Channel ch) {
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
