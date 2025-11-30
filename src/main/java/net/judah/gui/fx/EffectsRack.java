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
import net.judah.fx.EQ;
import net.judah.fx.Overdrive;
import net.judah.fx.Reverb;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.settable.PresetsHandler;
import net.judah.gui.widgets.FxKnob;
import net.judah.gui.widgets.Slider;
import net.judah.looper.Looper;
import net.judah.mixer.Channel;
import net.judah.util.RTLogger;

public class EffectsRack extends JPanel implements MPKTools {

    public static final int COLUMNS = 4;

    private final Channel channel;
    @Getter private final ChannelTitle title;
    @Getter private final PresetsHandler presets;

    private final ArrayList<Row> labels = new ArrayList<>();
    private final ArrayList<Row> knobs = new ArrayList<>();
    private final Slider.FxSlider phase;
    private final Slider.FxSlider dampness;
    private final Slider.FxSlider clipping;
    @Getter private final EQPlus eq;

    public EffectsRack(Channel ch, Looper looper) {
    	this.channel = ch;
        presets = new PresetsHandler(ch);
        title = new ChannelTitle(ch, looper);
        phase = new Slider.FxSlider(ch.getChorus(), Chorus.Settings.Phase.ordinal(), "Phase");
        dampness = new Slider.FxSlider(ch.getReverb(), Reverb.Settings.Damp.ordinal(), "Dampness");
        clipping = new Slider.FxSlider(ch.getOverdrive(), Overdrive.Settings.Clipping.ordinal(), "Clipping");
        eq = new EQPlus(ch);

		// wet  room   d.time  d.fb
		// cho1 cho2   cho3    O/D
        Row lbls = new Row(ch);
        ArrayList<Component> components = lbls.getControls();
        components.add(new FxTrigger("Reverb", ch.getReverb(), ch));
        components.add(Gui.resize(dampness, Size.TINY));
        components.add(new FxTrigger("Delay", ch.getDelay(), ch));
        components.add(new TimePanel(ch.getDelay(), ch));
        labels.add(lbls);

        lbls = new Row(ch);
        components = lbls.getControls();
        components.add(new FxTrigger("Dist.", ch.getOverdrive(), ch));
        components.add(Gui.resize(phase, Size.TINY));
        components.add(new FxTrigger("Chorus", ch.getChorus(), ch));
        components.add(new TimePanel(ch.getChorus(), ch));
        labels.add(lbls);

		// EQ L/M/H  Vol
		// Preset pArTy hiCut pan
        lbls = new Row(ch);
        components = lbls.getControls();
        components.add(Gui.wrap(Gui.resize(clipping, Size.MICRO)));
        components.add(new FxTrigger("   EQ   ", ch.getEq(), ch));
        components.add(eq.getToggle()); // new FxTrigger("     ", channel.getEq(), channel));
        components.add(new JLabel("Pan", JLabel.CENTER));
        labels.add(lbls);

        lbls = new Row(ch);
        components = lbls.getControls();
        components.add(presets);
        components.add(new FxTrigger("LoCut", ch.getLoCut(), ch));
        components.add(new FxTrigger("HiCut", ch.getHiCut(), ch));
        components.add(new FxTrigger("Volume", ch.getGain(), ch));
        labels.add(lbls);

        knobs.add(new RowKnobs(ch, 0));
        knobs.add(new RowKnobs(ch, 1));
        knobs.add(new RowKnobs(ch, eq));
        knobs.add(new RowKnobs(ch, 3));

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

    Component getKnob(int row, int col) {
    	return knobs.get(row).getControls().get(col);
    }

    Component getKnob(int idx) {
    	int col = idx % 4;
    	// 0-3   row 0, col 0-3
    	// 4-7   row 2, col 0-3
    	// 8-11  row 1, col 0-3
    	// 12-15 row 3, col 0-3
    	int firstOrder = idx / 8;
    	int compute = idx >= 8 ? idx - 8 : idx;
    	int secondOrder = (compute / 4) * 2;
    	int row = firstOrder + secondOrder;
    	return getKnob(row, col);
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
        eq.update();
        repaint();
    }

    /** amount effect changes for each encoder change */
    private static final int OFFSET = 2;

    public static int offset(int val, boolean up) {
    	val += up ? OFFSET : -OFFSET;
    	if (val > 100) val = 100;
    	if (val < 0) val = 0;
    	return val;
    }

    public void knob(int idx, boolean up) {

    	Component c = getKnob(idx);
    	if (c instanceof FxKnob fx)
    		fx.knob(up);
    	else if (idx == 4)
        	eq.knob(EQ.EqBand.Bass, up);
    	else if (idx == 5)
        	eq.knob(EQ.EqBand.Mid, up);
    	else if (idx == 6)
        	eq.knob(EQ.EqBand.High, up);
    	else if (c instanceof PresetsBtns)
    		presets.increment(up);
    	else {
    		RTLogger.log(c, "unknown: " + c + " " + c.getClass());
    		return;
    	}
    	MainFrame.update(channel);
    }

}


//switch(idx) {
//case 0:
//	int reverb = (int)(channel.getReverb().getWet() * 100f);
//	reverb = offset(reverb, up);
//	channel.getReverb().set(Reverb.Settings.Wet.ordinal(), reverb);
//	channel.getReverb().setActive(reverb > 0);
//	break;
//case 1:
//		int room = (int)(channel.getReverb().getRoomSize() * 100f);
//		room = offset(room, up);
//		channel.getReverb().set(Reverb.Settings.Room.ordinal(), room);
//		channel.getReverb().setActive(room > thresholdLo);
//	break;
//case 2:
//	int feedback = offset(channel.getDelay().get(Delay.Settings.Feedback.ordinal()), up);
//	channel.getDelay().set(Delay.Settings.Feedback.ordinal(), feedback);
//	channel.getDelay().setActive(feedback > 0);
//	break;
//case 3:
//	int time = offset(channel.getDelay().get(Delay.Settings.DelayTime.ordinal()), up);
//	channel.getDelay().set(Delay.Settings.DelayTime.ordinal(), time);
//	break;
//case 4:
//	eq.knob(EQ.EqBand.Bass, up);
//	break;
//case 5:
//	eq.knob(EQ.EqBand.Mid, up);
//	break;
//case 6:
//	eq.knob(EQ.EqBand.High, up);
//	break;
//case 7:
//	channel.getGain().set(Gain.PAN, offset(channel.getGain().get(Gain.PAN), up));
//    break;
//case 8:
//	Overdrive dist = channel.getOverdrive();
//	int od = offset(dist.get(0), up);
//	dist.set(0, od);
//    dist.setActive(od > 3);
//    break;
//case 9:
//	int depth = offset(channel.getChorus().get(Chorus.Settings.Depth.ordinal()), up);
//	channel.getChorus().set(Chorus.Settings.Depth.ordinal(), depth);
//	channel.getChorus().setActive(depth > thresholdLo);
//	break;
//case 10:
//	int fb = offset(channel.getChorus().get(Chorus.Settings.Feedback.ordinal()), up);
//	channel.getChorus().set(Chorus.Settings.Feedback.ordinal(), fb);
//	channel.getChorus().setActive(fb > thresholdLo);
//	break;
//case 11:
//	int rate = offset(channel.getChorus().get(Chorus.Settings.Rate.ordinal()), up);
//	channel.getChorus().set(Chorus.Settings.Rate.ordinal(), rate);
//	channel.getChorus().setActive(rate < thresholdHi);
//	break;
//case 12:
//	presets.increment(up);
//	break;
//case 13:
//	Filter hiCut = channel.getHiCut();
//	int hiNext = offset(hiCut.get(Filter.Settings.Hz.ordinal()), up);
//	hiCut.set(Filter.Settings.Hz.ordinal(), hiNext);
//	hiCut.setActive(hiNext < 100);
//	break;
//case 14:
//	Filter loCut = channel.getLoCut();
//	int loNext = offset(loCut.get(Filter.Settings.Hz.ordinal()), up);
//	loCut.set(Filter.Settings.Hz.ordinal(), loNext);
//	loCut.setActive(loNext > 0);
//	break;
//case 15:
//	channel.getGain().set(Gain.VOLUME, offset(channel.getVolume(), up));
//	break;
//}
