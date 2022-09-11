package net.judah.effects.gui;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPK;
import net.judah.looper.Loop;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.util.Constants;
import net.judah.util.GuitarTuner;
import net.judah.util.JudahMenu;
import net.judah.util.Pastels;

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
        for (Loop loop : JudahZone.getLooper()) 
            effectsTab.add(new EffectsRack(loop));
        for (LineIn input : JudahZone.getChannels()) {
        	EffectsRack rack = new EffectsRack(input);
        	effectsTab.add(rack);
        }
        	
        placeholder.addKeyListener(JudahMenu.getInstance());
        placeholder.setFocusTraversalKeysEnabled(false);
        placeholder.add(effectsTab.get(0));
        add(tuner);
        add(placeholder);
        doLayout();
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
