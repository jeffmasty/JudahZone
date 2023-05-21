package net.judah.mixer;

import java.awt.Component;
import java.awt.GridLayout;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.Sampler;
import net.judah.fx.Fader;
import net.judah.fx.LFO.Target;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.song.Cmdr;
import net.judah.song.FxData;
import net.judah.song.Param;
import net.judah.util.RTLogger;

/** Graphical representation of the Mixer*/
public class DJJefe extends JPanel implements Cmdr {

	@Getter private final ArrayList<Channel> all = new ArrayList<>();
	/** has GUI representation */
	@Getter private final ArrayList<Channel> channels = new ArrayList<>();
	@Getter private final String[] keys;
	private final ArrayList<MixWidget> faders = new ArrayList<MixWidget>();

    public DJJefe(Channel mains, Looper looper, Zone sources, List<DrumKit> kits, Sampler sampler) {
        
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
        keys = new String[channels.size()];
        for (int i = 0; i < keys.length; i++)
        	keys[i] = channels.get(i).getName();
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
			Channel ch = byName(fx.getChannel());
			if (ch == null) {
				RTLogger.warn(this, "Skipping Ch Fx: " + fx.getChannel());
				continue;
			}
			ch.setPreset(fx.getPreset());
		}
	}


//	@Override
//	public int value(String key) {
//		for (int i = 0; i < keys.length; i++)
//			if (keys[i].equals(key))
//				return i;
//		return 0;
//	}


	@Override
	public String lookup(int value) {
		if (value >= 0 && value < channels.size())
			return (channels.get(value).getName());
		return channels.get(0).getName();
	}


	@Override
	public Channel resolve(String key) {
		for (Channel ch : channels) 
			if (ch.getName().equals(key))
				return ch;
		return null;
	}


	@Override
	public void execute(Param p) {
		Channel ch = resolve(p.val);
		if (ch == null)
			return;
		switch (p.cmd) {
			case FadeOut:
				Fader.execute(new Fader(ch, Target.Gain, Fader.DEFAULT_FADE, ch.getVolume(), 0));
				break;
			case FadeIn:
				Fader.execute(new Fader(ch, Target.Gain, Fader.DEFAULT_FADE, 0, 51));
				break;
			case FX:
				ch.setPresetActive(!ch.isPresetActive());
				break;
			case Mute:
				ch.setOnMute(true);
				break;
			case Unmute: 
				ch.setOnMute(false);
				break;
			default: throw new InvalidParameterException("" + p);
		}
	}

}
