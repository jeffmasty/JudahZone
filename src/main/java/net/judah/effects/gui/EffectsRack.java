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
import net.judah.JudahZone;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPKTools;
import net.judah.effects.Chorus;
import net.judah.effects.CutFilter;
import net.judah.effects.CutFilter.Type;
import net.judah.effects.Delay;
import net.judah.effects.EQ;
import net.judah.effects.api.Preset;
import net.judah.effects.api.Reverb;
import net.judah.mixer.Channel;
import net.judah.util.Constants;
import net.judah.util.KeyPair;
import net.judah.util.Pastels;

public class EffectsRack extends JPanel implements GUI, MPKTools {
    // TODO lfo recover
    public static final String TAB_NAME = "Effects";
    public static final int COLUMNS = 4;
    
    @Getter private final Channel channel;
    private final ArrayList<RowLabels> labels = new ArrayList<>();
    private final ArrayList<RowKnobs> knobs = new ArrayList<>();
    private final JPanel rows;
    private final ChannelTitle title;
    private final PresetCombo presets;

    private final LFOGui lfo;

    public EffectsRack(Channel channel) {

        this.channel = channel;
        setBorder(BorderFactory.createLineBorder(Pastels.MY_GRAY));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        presets = new PresetCombo(channel, JudahZone.getPresets());
        
        title = new ChannelTitle(channel);
        add(title);
        
        GridBagLayout layout = new GridBagLayout();
        rows = new JPanel(layout);
        
        labels.add(new RowLabels(channel, KnobMode.FX1, new KeyPair[]{
        		new KeyPair("    ", channel.getReverb()),
        		new KeyPair("Reverb", channel.getReverb()),
        		new KeyPair("    ", channel.getReverb()),
        		new KeyPair("Vol.", channel.getGain())
        }));

        labels.add(new RowLabels(channel, KnobMode.FX1, new KeyPair[]{
	    		new KeyPair("    ", channel.getChorus()),
	    		new KeyPair("Chorus", channel.getChorus()),
	    		new KeyPair("    ", channel.getChorus()),
	    		new KeyPair("pArTy", channel.getCutFilter())
        }));

        labels.add(new RowLabels(channel, KnobMode.FX2, new KeyPair[]{
	    		new KeyPair("    ", channel.getDelay()),
	    		new KeyPair("Delay", channel.getDelay()),
	    		new KeyPair("Pan", channel.getGain()),
	    		new KeyPair("Dist.", channel.getOverdrive())

        }));
        
        labels.add(new RowLabels(channel, KnobMode.FX2, new KeyPair[]{
	    		new KeyPair("    ", channel.getEq()),
        		new KeyPair("EQ", channel.getEq()),
	    		new KeyPair("     ", channel.getEq()),
        		new KeyPair("FX", channel.getPreset())
        }));
        
        knobs.add(new RowKnobs(channel, KnobMode.FX1, 0));
        knobs.add(new RowKnobs(channel, KnobMode.FX1, 1));
        knobs.add(new RowKnobs(channel, KnobMode.FX2, 2));
        knobs.add(new RowKnobs(channel, KnobMode.FX2, presets));

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
        if (channel.getPreset() != null)
        	presets.setSelectedItem(channel.getPreset());
        repaint();
    }

    public void effects1(int data1, int data2) {
		if (data1 == KNOBS.get(0)) { 
			channel.getReverb().set(Reverb.Settings.Wet.ordinal(), data2);
			channel.getReverb().setActive(data2 > 0);
		}
        else if (data1 == KNOBS.get(1)) {
			channel.getReverb().set(Reverb.Settings.Room.ordinal(), data2); 
			channel.getReverb().setActive(data2 > thresholdLo);
		}
        else if (data1 == KNOBS.get(2)) {
        	channel.getReverb().set(Reverb.Settings.Damp.ordinal(), data2);
        }
        else if (data1 == KNOBS.get(3)) {
        	channel.getGain().setVol(data2);
        }
        
        else if (data1 == KNOBS.get(4)) {
        	channel.getChorus().set(Chorus.Settings.Rate.ordinal(), data2);
        	channel.getChorus().setActive(data2 < thresholdHi);
        }
        else if (data1 == KNOBS.get(5)) {
        	channel.getChorus().set(Chorus.Settings.Depth.ordinal(), data2);
        	channel.getChorus().setActive(data2 > thresholdLo);
        }
        else if (data1 == KNOBS.get(6)) {
        	channel.getChorus().set(Chorus.Settings.Feedback.ordinal(), data2);
        	channel.getChorus().setActive(data2 > thresholdLo);
        }
        else if (data1 == KNOBS.get(7)) {
        	channel.getCutFilter().setActive(data2 < thresholdHi);
        	if (!channel.getCutFilter().isActive()) return;
        	CutFilter party = channel.getCutFilter();
        	party.setFilterType(Type.pArTy);
        	party.setFrequency(CutFilter.knobToFrequency(data2));
        }
	}

	public void effects2(int data1, int data2) {
		if (data1 == KNOBS.get(0)) {
        	channel.getDelay().set(Delay.Settings.DelayTime.ordinal(), data2);
        	channel.getDelay().setActive(data2 < thresholdHi);
        }
        else if (data1 == KNOBS.get(1)) {
        	channel.getDelay().set(Delay.Settings.Feedback.ordinal(), data2);
        	channel.getDelay().setActive(data2 > 0);
        }
        else if (data1 == KNOBS.get(2)) {
        	channel.getGain().setPan(data2);
        }
        else if (data1 == KNOBS.get(3)) {
        	channel.getOverdrive().setDrive(Constants.logarithmic(data2, 0, 1));
            channel.getOverdrive().setActive(data2 > 0);
        }

        else if (data1 == KNOBS.get(4)) { 
			channel.getEq().eqGain(EQ.EqBand.Bass, data2);
			channel.getEq().setActive(data2 > thresholdLo);
		}
        else if (data1 == KNOBS.get(5)) {
			channel.getEq().eqGain(EQ.EqBand.Mid, data2);
			channel.getEq().setActive(data2 > thresholdLo);
		}
        else if (data1 == KNOBS.get(6)) {
			channel.getEq().eqGain(EQ.EqBand.High, data2);
			channel.getEq().setActive(data2 > thresholdLo);
        }
        else if (data1 == KNOBS.get(7)) {
        	Preset preset = (Preset)Constants.ratio(data2, JudahZone.getPresets());
        	channel.setPreset(preset);
        }
	}

}

