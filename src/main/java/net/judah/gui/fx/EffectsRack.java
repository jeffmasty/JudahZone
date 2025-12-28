package net.judah.gui.fx;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.api.Effect;
import net.judah.controllers.MPKTools;
import net.judah.fx.Chorus;
import net.judah.fx.Delay;
import net.judah.fx.EQ;
import net.judah.fx.Overdrive;
import net.judah.fx.Reverb;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.fx.ReverbPlus.UpdatePanel;
import net.judah.gui.settable.PresetsHandler;
import net.judah.gui.widgets.FxKnob;
import net.judah.gui.widgets.Slider;
import net.judah.midi.MidiInstrument;
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
    @Getter private final ReverbPlus reverb;
    private final OD drive;
    private final TimePanel chorusTime;
    private final TimePanel delayTime;

    public EffectsRack(Channel ch, MidiInstrument bass) {
    	this.channel = ch;
        presets = new PresetsHandler(channel);
        title = new ChannelTitle(channel, bass);
        phase = new Slider.FxSlider(ch.getChorus(), Chorus.Settings.Phase.ordinal(), "Phase");
        dampness = new Slider.FxSlider(ch.getReverb(), Reverb.Settings.Width.ordinal(), "Dampness");
        clipping = new Slider.FxSlider(ch.getOverdrive(), Overdrive.Settings.Clipping.ordinal(), "Clipping");
        eq = new EQPlus(channel);
        drive = new OD(channel);
        reverb = new ReverbPlus(ch);
        delayTime = new TimePanel(ch.getDelay(), ch);
        chorusTime = new TimePanel(ch.getChorus(), ch);

		// wet  room   d.time  d.fb
		// O/D  cho1    cho2   cho3
        Row lbls = new Row(ch);
        ArrayList<Component> components = lbls.getControls();
        components.add(new FxTrigger("Reverb", ch.getReverb(), ch));
        components.add(new FxTrigger(" ", ch.getReverb(), ch)); // TODO rev+
        components.add(new FxTrigger("Delay", ch.getDelay(), ch));
        components.add(delayTime);
        labels.add(lbls);

        lbls = new Row(ch);
        components = lbls.getControls();

        JPanel od = Gui.wrap(new FxTrigger("O/D", ch.getOverdrive(), ch),
        		Gui.resize(drive, Size.MODE_SIZE));
        components.add(od);
        components.add(Gui.resize(phase, Size.TINY));
        components.add(new FxTrigger("Chorus", ch.getChorus(), ch));
        components.add(chorusTime);
        labels.add(lbls);

		// EQ L/M/H  Vol
		// Preset pArTy hiCut pan
        lbls = new Row(ch);
        components = lbls.getControls();
        components.add(Gui.wrap(Gui.resize(clipping, Size.MICRO)));
        components.add(new FxTrigger("   EQ   ", ch.getEq(), ch));
        components.add(eq.getToggle());
        components.add(new JLabel("Pan", JLabel.CENTER));
        labels.add(lbls);

        lbls = new Row(ch);
        components = lbls.getControls();
        components.add(presets);
        components.add(new FxTrigger("LoCut", ch.getLoCut(), ch));
        components.add(new FxTrigger("HiCut", ch.getHiCut(), ch));
        components.add(new FxTrigger("Volume", ch.getGain(), ch));
        labels.add(lbls);

        knobs.add(new RowKnobs(ch, reverb));
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

	public void update(Effect fx) {

		// TODO time widget + LFO Time Widgets

		if (fx instanceof Reverb) {
			dampness.update();
			reverb.update();
			return;
		}

		if (fx instanceof EQ) {
			eq.update();
			return;
		}

		if (fx instanceof OD)
			clipping.update();
		else if (fx instanceof Delay) {
			delayTime.update();
		}
		else if (fx instanceof Chorus) {
			phase.update();
			chorusTime.update();
		}

		for (Row row: knobs)
			for (Component c : row.getControls())
				if (c instanceof FxKnob knob && knob.getEffect() == fx)
					knob.update();
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
    	else if (c instanceof UpdatePanel ok)
    		ok.getKnob().knob(up);
    	else if (idx == 4)
        	eq.knob(EQ.EqBand.Bass, up);
    	else if (idx == 5)
        	eq.knob(EQ.EqBand.Mid, up);
    	else if (idx == 6)
        	eq.knob(EQ.EqBand.High, up);
    	else if (c instanceof PresetsBtns)
    		presets.increment(up);
    	else {
    		RTLogger.warn(c, "unknown: " + c + " " + c.getClass());
    		return;
    	}
    	MainFrame.update(channel);
    }

    private class OD extends JComboBox<Overdrive.Algo> {
    	OD(final Channel ch) {
    		super(Overdrive.Algo.values());
    		setSelectedItem(Overdrive.Algo.SMITH);
    		addActionListener(e -> ch.getOverdrive().set(
            		Overdrive.Settings.Algo.ordinal(), getSelectedIndex()));
    	}
    }

	public void updatePreset() {
		presets.update();
		for (Component c : knobs.get(3).getControls()) {
			if (c instanceof PresetsBtns btns)
				btns.update();
		}
	}


}
