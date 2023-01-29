package net.judah.gui.fx;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.controllers.MPKTools;
import net.judah.fx.*;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.Fx;
import net.judah.mixer.Channel;
import net.judah.util.KeyPair;

public class EffectsRack extends JPanel implements MPKTools {
	
    public static final String TAB_NAME = "Effects";
    public static final int COLUMNS = 4;
    
    @Getter private final Channel channel;
    private final ArrayList<RowLabels> labels = new ArrayList<>();
    private final ArrayList<Row> knobs = new ArrayList<>();
    private final JPanel rows;
    @Getter private final ChannelTitle title;
    
    public EffectsRack(Channel channel) {

        this.channel = channel;
        title = new ChannelTitle(channel);
		// wet  room   d.time  d.fb
		// cho1 cho2   cho3    O/D
        labels.add(new RowLabels(channel, new KeyPair[]{
        		new KeyPair("    ", channel.getReverb()),
        		new KeyPair("Reverb", channel.getReverb()),
        		new KeyPair("Delay", channel.getDelay()),
        		new KeyPair("    ", channel.getDelay())}));

        labels.add(new RowLabels(channel, new KeyPair[]{
	    		new KeyPair("    ", channel.getChorus()),
        		new KeyPair("Chorus", channel.getChorus()),
	    		new KeyPair("    ", channel.getChorus()),
	    		new KeyPair("Dist.", channel.getOverdrive())}));

		// EQ L/M/H  Vol
		// Preset pArTy hiCut pan 
        labels.add(new RowLabels(channel, new KeyPair[]{
        		new KeyPair("     ", channel.getEq()),
        		new KeyPair("EQ     ", channel.getEq()),
	    		new KeyPair("     ", channel.getEq()),
        		new KeyPair("Volume", channel.getGain())}));

        labels.add(new RowLabels(channel, new KeyPair[]{
	    		new KeyPair("Preset", null),
	    		new KeyPair("pArTy", channel.getCutFilter()),
	    		new KeyPair("HiCut", channel.getHiCut()),
	    		new KeyPair("Pan", channel.getGain())}));

        knobs.add(new RowKnobs(channel, 0));
        knobs.add(new RowKnobs(channel, 1));
        knobs.add(new RowKnobs(channel, 2));
        knobs.add(new RowKnobs(channel, 3));
        
        GridBagLayout layout = new GridBagLayout();
        rows = new JPanel(layout);
        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = 0;
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

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(title);
        add(rows);
        
    }

    
    public void update() {
        title.update();
        for (RowLabels lbl : labels) 
        	lbl.update();
        for (Row knob : knobs)
        	knob.update();
        repaint();
    }

    int offset = 3;

    private int offset(int val, boolean up) {
    	val += up ? offset : -offset;
    	if (val > 100) val = 100;
    	if (val < 0) val = 0;
    	return val;
    }
    
    public void knob(int idx, boolean up) {
    	switch(idx) {
    	case 0:
    		int reverb = (int)(channel.getReverb().getWet() * 100f);
    		reverb = offset(reverb, up);
    		channel.getReverb().set(Reverb.Settings.Wet.ordinal(), reverb);
			channel.getReverb().setActive(reverb > 0);
			break;
    	case 1: 
    		int room = (int)(channel.getReverb().getRoomSize() * 100f);
    		room = offset(room, up);
    		channel.getReverb().set(Reverb.Settings.Room.ordinal(), room);
			channel.getReverb().setActive(room > thresholdLo);
			break;
		case 2:
        	int feedback = offset(channel.getDelay().get(Delay.Settings.Feedback.ordinal()), up);
        	channel.getDelay().set(Delay.Settings.Feedback.ordinal(), feedback);
        	channel.getDelay().setActive(feedback > 0);
			break;
        case 3:
        	int time = offset(channel.getDelay().get(Delay.Settings.DelayTime.ordinal()), up);
        	channel.getDelay().set(Delay.Settings.DelayTime.ordinal(), time);
        	break;
        case 4:
        	eq(EQ.EqBand.Bass, up);
			break;
        case 5:
        	eq(EQ.EqBand.Mid, up);
        	break;
        case 6:    	
        	eq(EQ.EqBand.High, up);
        	break;
        case 7:
        	channel.getGain().setVol(offset(channel.getGain().getVol(), up));
            break;
        case 8:
        	int rate = offset(channel.getChorus().get(Chorus.Settings.Rate.ordinal()), up);
        	channel.getChorus().set(Chorus.Settings.Rate.ordinal(), rate);
        	channel.getChorus().setActive(rate < thresholdHi);
			break;
        case 9:
        	int depth = offset(channel.getChorus().get(Chorus.Settings.Depth.ordinal()), up);
        	channel.getChorus().set(Chorus.Settings.Depth.ordinal(), depth);
        	channel.getChorus().setActive(depth > thresholdLo);
			break;
        case 10:
        	int fb = offset(channel.getChorus().get(Chorus.Settings.Feedback.ordinal()), up);
        	channel.getChorus().set(Chorus.Settings.Feedback.ordinal(), fb);
        	channel.getChorus().setActive(fb > thresholdLo);
			break;
        case 11:
        	Overdrive dist = channel.getOverdrive();
        	int od = offset(dist.get(0), up);
        	dist.set(0, od);
            dist.setActive(od > 0);
            break;
        case 12:			
        	((Fx)knobs.get(3).getControls().get(0)).increment(up); // not elegant
        	break;
        case 13:
        	CutFilter filter = channel.getCutFilter();
        	int freak = CutFilter.frequencyToKnob(filter.getFrequency());
        	freak = offset(freak, up);
        	filter.setActive(freak < thresholdHi);
        	if (!filter.isActive()) return;
        	filter.setFilterType(CutFilter.Type.pArTy);
        	filter.setFrequency(CutFilter.knobToFrequency(freak));
			break;
        case 14:
        	CutFilter hello = channel.getHiCut();
        	int hz = offset(CutFilter.frequencyToKnob(hello.getFrequency()), up);
        	hello.setActive(hz < thresholdHi);
        	if (!hello.isActive()) return;
        	hello.setFrequency(CutFilter.knobToFrequency(hz));
        	break;
        case 15:  
        	channel.getGain().setPan(offset(channel.getGain().getPan(), up));
        	break;
    	}
    	MainFrame.update(channel);
    }
    
    private void eq(EQ.EqBand band, boolean up) {
    	int db = offset(channel.getEq().get(band.ordinal()), up);
    	channel.getEq().eqGain(band, db);
    	channel.getEq().setActive(db > thresholdLo);
    }

}
