package net.judah.mixer;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.Sampler;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.song.FxData;
import net.judah.util.RTLogger;

/** Graphical representation of the Mixer*/
public class DJJefe extends JPanel {

	@Getter private final ArrayList<Channel> all = new ArrayList<>();
	/** has GUI representation */
	@Getter private final ArrayList<Channel> channels = new ArrayList<>();
	private final ArrayList<MixWidget> faders = new ArrayList<MixWidget>();

    public DJJefe(Channel mains, Looper looper, Zone sources, DrumKit[] kits, Sampler sampler) {
        
    	all.addAll(looper);
		all.addAll(sources);
		for (DrumKit k : kits)
			all.add(k);
		all.addAll(sampler);
		all.addAll(sampler.getStepSamples());
    	
    	for (Loop loop : looper) {
    		channels.add(loop);
    		MixWidget fader = new LoopMix(loop, looper);
    		faders.add(fader);
    		add(fader);
    	}
        for (LineIn instrument : sources) {
        	channels.add(instrument);
    		MixWidget fader = new LineMix(instrument, looper.getSoloTrack());
    		faders.add(fader);
    		add(fader);
        }
        channels.add(mains);
        MixWidget fader = new MainsMix(mains);
        faders.add(fader);
        add(fader);
        
    	setLayout(new GridLayout(1, channels.size()));
        doLayout();
    }

    
    public void addChannel(Channel ch) {
    	for (Channel already : channels)
    		if (already == ch)
    			return;
	    channels.add(ch);
    }
	    
    public void removeChannel(Channel ch) {
	    if (!channels.contains(ch)) 
		    return;
	    MixWidget fade = getFader(ch);
	    remove(fade);
	    faders.remove(fade);
	    channels.remove(ch);
    }
 	    
	public void update(Channel channel) {
		for (MixWidget ch : faders) 
			if (ch.getChannel().equals(channel)) 
				ch.update();
	}

	public void updateAll() {
		for (MixWidget ch : faders) 
			ch.update();
	}

	public void highlight(ArrayList<Channel> s) {
		for (MixWidget ch : faders) {
			ch.setBorder(s.contains(ch.getChannel()) ? Gui.HIGHLIGHT : Gui.NO_BORDERS);
		}
	}
	
	public void highlight(Channel o) {
		for (MixWidget ch : faders) 
			ch.setBorder(ch.getChannel() == o ? Gui.HIGHLIGHT : Gui.NO_BORDERS);
	}

	public MixWidget getFader(Channel ch) {
		for (MixWidget fade : faders)
			if (fade.getChannel() == ch)
				return fade;
		return null;
	}
	
	public Channel byName(String channel) {
		for (Channel ch : all)
			if (ch.getName().equals(channel))
				return ch;
		return null;
	}

	private boolean comboOverride;
	private JComboBox<Channel> combo;
	public Component getCombo(Channel selected) {
		if (combo == null) {
			ArrayList<Channel> chz = getAll();
	    	combo = new JComboBox<>(chz.toArray(new Channel[chz.size()]));
	    	combo.setFont(Gui.BOLD13);
	    	combo.addActionListener(e-> {
	    		if (!comboOverride) 
	    			MainFrame.setFocus(((Channel)combo.getSelectedItem()).getLfoKnobs());});
		}
		comboOverride = true;
		combo.setSelectedItem(selected);
		comboOverride = false;
		return combo;
	}


	public void loadFx(List<FxData> data) {
		for (FxData fx : data) {
			Channel ch = JudahZone.getMixer().byName(fx.getChannel());
			if (ch == null) {
				RTLogger.warn(this, "Skipping Ch Fx: " + fx.getChannel());
				continue;
			}
			ch.setPreset(fx.getPreset());
		}
	}

}
