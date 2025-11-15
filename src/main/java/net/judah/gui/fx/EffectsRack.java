package net.judah.gui.fx;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.controllers.MPKTools;
import net.judah.fx.Chorus;
import net.judah.fx.Filter;
import net.judah.fx.Delay;
import net.judah.fx.EQ;
import net.judah.fx.Gain;
import net.judah.fx.Overdrive;
import net.judah.fx.Reverb;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.settable.PresetsHandler;
import net.judah.gui.widgets.Slider;
import net.judah.looper.Looper;
import net.judah.mixer.Channel;

public class EffectsRack extends JPanel implements MPKTools {

    public static final int COLUMNS = 4;

    @Getter private final Channel channel;
    @Getter private final ChannelTitle title;
    @Getter private final PresetsHandler presets;

    private final ArrayList<Row> labels = new ArrayList<>();
    private final ArrayList<Row> knobs = new ArrayList<>();
    private final Slider.FxSlider phase;
    private final Slider.FxSlider dampness;
    private final Slider.FxSlider clipping;

    public EffectsRack(Channel channel, Looper looper) {
        this.channel = channel;
        presets = new PresetsHandler(channel);
        title = new ChannelTitle(channel, looper);
        phase = new Slider.FxSlider(channel.getChorus(), Chorus.Settings.Phase.ordinal(), "Phase");
        dampness = new Slider.FxSlider(channel.getReverb(), Reverb.Settings.Damp.ordinal(), "Dampness");
        clipping = new Slider.FxSlider(channel.getOverdrive(), Overdrive.Settings.Clipping.ordinal(), "Clipping");

		// wet  room   d.time  d.fb
		// cho1 cho2   cho3    O/D
        Row lbls = new Row(channel);
        ArrayList<Component> components = lbls.getControls();
        components.add(new FxTrigger("Reverb", channel.getReverb(), channel));
        components.add(Gui.resize(dampness, Size.TINY));
        components.add(new FxTrigger("Delay", channel.getDelay(), channel));
        components.add(new TimePanel(channel.getDelay(), channel));
        labels.add(lbls);

        lbls = new Row(channel);
        components = lbls.getControls();
        components.add(new FxTrigger("Dist.", channel.getOverdrive(), channel));
        components.add(Gui.resize(phase, Size.TINY));
        components.add(new FxTrigger("Chorus", channel.getChorus(), channel));
        components.add(new TimePanel(channel.getChorus(), channel));
        labels.add(lbls);

		// EQ L/M/H  Vol
		// Preset pArTy hiCut pan
        lbls = new Row(channel);
        components = lbls.getControls();
        components.add(Gui.wrap(Gui.resize(clipping, Size.MICRO)));
        components.add(new FxTrigger("   EQ   ", channel.getEq(), channel));
        components.add(new FxTrigger("     ", channel.getEq(), channel));
        components.add(new JLabel("Pan", JLabel.CENTER));
        labels.add(lbls);

        lbls = new Row(channel);
        components = lbls.getControls();
        components.add(presets);
        components.add(new FxTrigger("HiCut", channel.getHiCut(), channel));
        components.add(new FxTrigger("LoCut", channel.getLoCut(), channel));
        components.add(new FxTrigger("Volume", channel.getGain(), channel));
        labels.add(lbls);

        knobs.add(new RowKnobs(channel, 0));
        knobs.add(new RowKnobs(channel, 1));
        knobs.add(new RowKnobs(channel, 2));
        knobs.add(new RowKnobs(channel, 3));

        GridBagLayout layout = new GridBagLayout();
        JPanel rows = new JPanel(layout);
        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        for (int y = 0; y < knobs.size() * 2; y++)
        	for (int x = 0; x < COLUMNS; x++) {
    			c.gridx = x;
    			c.gridy = y;
    			Component widget = (y % 2 == 0) ?
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
        for (Row lbl : labels)
        	lbl.update();
        for (Row knob : knobs)
        	knob.update();
        phase.update();
        dampness.update();
        clipping.update();
        repaint();
    }

    /** amount effect changes for each encoder change */
    private static final int OFFSET = 3;

    private int offset(int val, boolean up) {
    	val += up ? OFFSET : -OFFSET;
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
        	channel.getGain().set(Gain.PAN, offset(channel.getGain().get(Gain.PAN), up));
            break;
        case 8:
        	Overdrive dist = channel.getOverdrive();
        	int od = offset(dist.get(0), up);
        	dist.set(0, od);
            dist.setActive(od > 3);
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
        	int rate = offset(channel.getChorus().get(Chorus.Settings.Rate.ordinal()), up);
        	channel.getChorus().set(Chorus.Settings.Rate.ordinal(), rate);
        	channel.getChorus().setActive(rate < thresholdHi);
			break;
        case 12:
        	presets.increment(up);
        	break;
        case 13:
        	Filter hiCut = channel.getHiCut();
        	int hiNext = offset(hiCut.get(Filter.Settings.Hz.ordinal()), up);
//        	int joystick = offset(hiCut.get(DJFilter.Settings.Joystick.ordinal()), up);
        	hiCut.set(Filter.Settings.Hz.ordinal(), hiNext);
        	hiCut.setActive(hiNext < 100);
        	break;
        case 14:
        	Filter loCut = channel.getLoCut();
        	int loNext = offset(loCut.get(Filter.Settings.Hz.ordinal()), up);
        	loCut.set(Filter.Settings.Hz.ordinal(), loNext);
//        	FilterType type = loCut.get(CutFilter.Settings.Type.ordinal()) > 0 ? FilterType.HighPass : FilterType.LowPass;
//        	if (type == FilterType.HighPass)
        		loCut.setActive(loNext > 0);
//        	else
//        		loCut.setActive(loNext < 100);
        	break;
        case 15:
        	channel.getGain().set(Gain.VOLUME, offset(channel.getVolume(), up));
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
