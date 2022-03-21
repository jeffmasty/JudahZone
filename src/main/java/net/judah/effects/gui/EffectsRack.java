package net.judah.effects.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.controllers.KnobMode;
import net.judah.mixer.Channel;
import net.judah.util.KeyPair;
import net.judah.util.Pastels;

public class EffectsRack extends JPanel implements GUI {
    // TODO lfo recover
    public static final String TAB_NAME = "Effects";
    public static final int COLUMNS = 4;
    
    @Getter private final Channel channel;
    private final ArrayList<RowLabels> labels = new ArrayList<>();
    private final ArrayList<RowKnobs> knobs = new ArrayList<>();
    private final JPanel rows;
    private final ChannelTitle title;

    private final LFOGui lfo;

    public EffectsRack(Channel channel) {

        this.channel = channel;
        setBorder(BorderFactory.createLineBorder(Pastels.MY_GRAY));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        title = new ChannelTitle(channel);
        add(title);
        
        GridBagLayout layout = new GridBagLayout();
        rows = new JPanel(layout);
        
        labels.add(new RowLabels(channel, KnobMode.Effects1, new KeyPair[]{
        		new KeyPair("", channel.getReverb()),
        		new KeyPair("Reverb", channel.getReverb()),
        		new KeyPair("", channel.getReverb()),
        		new KeyPair("Dist.", channel.getOverdrive()),
        }));

        labels.add(new RowLabels(channel, KnobMode.Effects1, new KeyPair[]{
	    		new KeyPair("", channel.getChorus()),
	    		new KeyPair("Chorus", channel.getChorus()),
	    		new KeyPair("", channel.getChorus()),
	    		new KeyPair("pArTy", channel.getCutFilter())
        }));

        labels.add(new RowLabels(channel, KnobMode.Effects2, new KeyPair[]{
	    		new KeyPair("", channel.getEq()),
	    		new KeyPair("EQ", channel.getEq()),
	    		new KeyPair("", channel.getEq()),
	    		new KeyPair("Delay", channel.getDelay()),
        }));
        
        labels.add(new RowLabels(channel, KnobMode.Effects2, new KeyPair[]{
	    		new KeyPair("Volume", channel.getGain()),
	    		new KeyPair("Pan", channel.getGain()),
	    		new KeyPair("Comp.", channel.getCompression()),
	    		new KeyPair("", channel.getDelay()),
        }));
        
        knobs.add(new RowKnobs(channel, KnobMode.Effects1, 0));
        knobs.add(new RowKnobs(channel, KnobMode.Effects1, 1));
        knobs.add(new RowKnobs(channel, KnobMode.Effects2, 2));
        knobs.add(new RowKnobs(channel, KnobMode.Effects2, 3));

        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        for (int y = 0; y < knobs.size() * 2; y++) 
        	for (int x = 0; x < COLUMNS; x++) {
    			c.gridx = x;
    			c.gridy = y;
    			Component widget = (y %2 == 0) ? 
    					labels.get(y / 2).getControls().get(x) :
    					knobs.get(y / 2).getControls().get(x);
    			layout.setConstraints(widget, c);
    			rows.add(widget);
        	}
        add(rows);
        add(new JLabel(" ")); // filler
        lfo = new LFOGui(channel);
        add(lfo);
        
    }

    public void update() {
        title.update();
        for (RowLabels lbl : labels) 
        	lbl.update();
        for (RowKnobs knob : knobs)
        	knob.update();

        repaint();
    }

}

