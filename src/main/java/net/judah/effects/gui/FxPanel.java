package net.judah.effects.gui;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPKmini;
import net.judah.mixer.Channel;
import net.judah.util.Constants;
import net.judah.util.GuitarTuner;
import net.judah.util.JudahMenu;
import net.judah.util.Pastels;

public class FxPanel extends JPanel {

    @Getter private EffectsRack current;
    @Getter private final GuitarTuner tuner = new GuitarTuner();
    private JPanel placeholder = new JPanel();
    
    public FxPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(Constants.Gui.NONE);
        setBackground(Pastels.EGGSHELL);
        current = JudahZone.getMains().getGui();

        placeholder.addKeyListener(JudahMenu.getInstance());
        placeholder.setFocusTraversalKeysEnabled(false);
        placeholder.add(current);
        add(placeholder);
        add(tuner);
        doLayout();
    }

    public void setFocus(Channel ch) {
    	MPKmini.setMode(KnobMode.FX1);
    	if (ch.equals(getChannel())) {
    		return;
    	}
    	current = ch.getGui();
    	new Thread(()->{
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