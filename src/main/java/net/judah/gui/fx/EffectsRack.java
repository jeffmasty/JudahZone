package net.judah.gui.fx;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import judahzone.api.FX;
import judahzone.fx.Chorus;
import judahzone.fx.Delay;
import judahzone.fx.EQ;
import judahzone.fx.Gain;
import judahzone.fx.MonoFilter;
import judahzone.fx.Overdrive;
import judahzone.fx.Reverb;
import judahzone.gui.Gui;
import judahzone.gui.Updateable;
import judahzone.util.RTLogger;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.channel.PresetsHandler;
import net.judah.controllers.MPKTools;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.fx.ReverbPlus.UpdatePanel;
import net.judah.gui.widgets.FxKnob;
import net.judah.gui.widgets.Slider;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;

public class EffectsRack extends JPanel implements MPKTools {

    public static final int COLUMNS = 4;

    private final Channel channel;
    @Getter private final ChannelTitle title;
    @Getter private final PresetsHandler presets;
    @Getter private final EQPlus eq;
    @Getter private final ReverbPlus reverb;
    private final Slider.FxSlider phase;
    private final Slider.FxSlider dampness;
    private final Slider.FxSlider clipping;
    private final OD drive;
    private final TimePanel chorusTime;
    private final TimePanel delayTime;

    private final ArrayList<ArrayList<Component>> labels = new ArrayList<>();
    private final ArrayList<Row> knobs = new ArrayList<>();

    public EffectsRack(Channel ch, JudahZone judahZone) {
    	this.channel = ch;
        presets = new PresetsHandler(channel);
        title = new ChannelTitle(channel, judahZone);
        phase = new Slider.FxSlider(ch.getChorus(), Chorus.Settings.Phase.ordinal(), "Phase");
        dampness = new Slider.FxSlider(ch.getReverb(), Reverb.Settings.Width.ordinal(), "Dampness");
        clipping = new Slider.FxSlider(ch.getOverdrive(), Overdrive.Settings.Clipping.ordinal(), "Clipping");
        eq = new EQPlus(channel);
        drive = new OD(channel);
        reverb = new ReverbPlus(ch);
        JudahClock clock = JudahMidi.getClock();
        delayTime = new TimePanel(ch.getDelay(), ch, clock);
        chorusTime = new TimePanel(ch.getChorus(), ch, clock);

        labels();
        knobs.add(row0());
        knobs.add(row1());
        knobs.add(row2());
        knobs.add(row3());

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
    					labels.get(y / 2).get(x) :
    					knobs.get(y / 2).get(x);
    			layout.setConstraints(widget, c);
    			rows.add(widget);
        	}
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(title);
        add(rows);
    }

    Point refactor(int idx) {
    	// 0-3   row 0, col 0-3
    	// 4-7   row 2, col 0-3
    	// 8-11  row 1, col 0-3
    	// 12-15 row 3, col 0-3
    	int col = idx % 4;
    	int firstOrder = idx / 8;
    	int compute = idx >= 8 ? idx - 8 : idx;
    	int secondOrder = (compute / 4) * 2;
    	int row = firstOrder + secondOrder;
    	return new Point(row, col);
    }

    FX getEffect(int idx) {
    	Point p = refactor(idx);
    	return knobs.get(p.x).getFx(p.y);
    }

    Component getKnob(int row, int col) {
    	return knobs.get(row).get(col);
    }

    Component getKnob(int idx) {
    	Point p = refactor(idx);
    	return getKnob(p.x, p.y);
    }

	public void update() {
        title.update();
        for (Row row : knobs)
        	row.update();
        phase.update();
        dampness.update();
        clipping.update();
        eq.update();
        repaint();
    }

	public void update(FX fx) {

		if (fx == null)
			updatePreset();

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
			for (Component c : row.list())
				if (c instanceof FXAware knob && knob.getFx() == fx)
					if (c instanceof Updateable up)
						up.update();
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

    	if (c instanceof FXAware aware)
    		MainFrame.updateFx(channel, aware.getFx());
    	else // ?
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
		for (Component c : knobs.get(3).list()) {
			if (c instanceof PresetsBtns btns)
				btns.update();
		}
	}

	private void labels() {
		Channel ch = channel;
		// wet  room   d.time  d.fb
		// O/D  cho1    cho2   cho3
        ArrayList<Component> lbls0 = new ArrayList<>();
        lbls0.add(new FxTrigger("Reverb", ch.getReverb(), ch));
        lbls0.add(new FxTrigger(" ", ch.getReverb(), ch)); // TODO rev+
        lbls0.add(new FxTrigger("Delay", ch.getDelay(), ch));
        lbls0.add(delayTime);
        labels.add(lbls0);

        JPanel od = Gui.wrap(new FxTrigger("O/D", ch.getOverdrive(), ch),
        		Gui.resize(drive, Size.MODE_SIZE));

        ArrayList<Component> lbls1 = new ArrayList<>();
        lbls1.add(od);
        lbls1.add(Gui.resize(phase, Size.TINY));
        lbls1.add(new FxTrigger("Chorus", ch.getChorus(), ch));
        lbls1.add(chorusTime);
        labels.add(lbls1);

		// EQ L/M/H  Vol
		// Preset pArTy hiCut pan
        ArrayList<Component> lbls2 = new ArrayList<>();
        lbls2.add(Gui.wrap(Gui.resize(clipping, Size.MICRO)));
        lbls2.add(new FxTrigger("   EQ   ", ch.getEq(), ch));
        lbls2.add(eq.getToggle());
        lbls2.add(new JLabel("Pan", JLabel.CENTER));
        labels.add(lbls2);

        ArrayList<Component> lbls3 = new ArrayList<>();
        lbls3.add(presets);
        lbls3.add(new FxTrigger("LoCut", ch.getLoCut(), ch));
        lbls3.add(new FxTrigger("HiCut", ch.getHiCut(), ch));
        lbls3.add(new FxTrigger("Volume", ch.getGain(), ch));
        labels.add(lbls3);
	}

	private Row row0() {
		Row row = new Row(channel);
		row.add(reverb.getLeft());
		row.add(reverb.getRight());
		row.add(new FxKnob(channel, channel.getDelay(), Delay.Settings.Feedback.ordinal(),
				"F/B", Delay.Settings.Type.ordinal()));
		row.add(new FxKnob(channel, channel.getDelay(), Delay.Settings.DelayTime.ordinal(),
				"Time", Delay.Settings.Sync.ordinal()));
		return row;
	}

	private Row row1() {
		Row row = new Row(channel);
		row.add(new FxKnob(channel, channel.getOverdrive(), Overdrive.Settings.Drive.ordinal(),
				"Gain", Overdrive.Settings.Clipping.ordinal()));
		row.add(new FxKnob(channel, channel.getChorus(), Chorus.Settings.Depth.ordinal(),
				Chorus.Settings.Depth.name(), Chorus.Settings.Phase.ordinal()));
		row.add(new FxKnob(channel, channel.getChorus(), Chorus.Settings.Feedback.ordinal(),
				"F/B", Chorus.Settings.Type.ordinal()));
		row.add(new FxKnob(channel, channel.getChorus(), Chorus.Settings.Rate.ordinal(),
				Chorus.Settings.Rate.name(), Chorus.Settings.Sync.ordinal(), true));
		return row;
	}

	private Row row2() {
		Row row = new Row(channel);
		row.add(eq.getLeft());
		row.add(eq.getCenter());
		row.add(eq.getRight());
		row.add(new FxKnob(channel, channel.getGain(), Gain.PAN, ""));
		return row;
	}

	private Row row3() {
		Row row = new Row(channel);
		row.add(new PresetsBtns(channel));
		row.add(new FxKnob(channel, channel.getLoCut(), MonoFilter.Settings.Frequency.ordinal(), "Hz."));
		row.add(new FxKnob(channel, channel.getHiCut(), MonoFilter.Settings.Frequency.ordinal(), "Hz.", true));
		row.add(new FxKnob(channel, channel.getGain(), Gain.VOLUME, ""));
		return row;
	}


}
