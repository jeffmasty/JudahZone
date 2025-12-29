package net.judah.gui.knobs;

import static net.judah.fx.LFO.Settings.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.fx.LFO;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.fx.FxTrigger;
import net.judah.gui.fx.Row;
import net.judah.gui.fx.TimePanel;
import net.judah.gui.settable.LfoCombo;
import net.judah.gui.widgets.DoubleSlider;
import net.judah.gui.widgets.FxKnob;
import net.judah.gui.widgets.RangeSlider.Colors;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.util.Constants;

public class LFOWidget extends Box {
    private static final Color KNOB_C = Pastels.EGGSHELL;
    public static final Colors RANGE = new Colors(KNOB_C, KNOB_C, Pastels.PURPLE);

	@Getter private final LFO lfo;
	private final Channel ch;

	private final Row row;
	private final LfoCombo lfoCombo;
	private final DoubleSlider slider;
	private final FxKnob msec;
	private final JPanel wrap = new JPanel();
    final JPanel lfoLbls =  new JPanel(new GridLayout(1, 4, 0, 1));
    GridBagLayout layout = new GridBagLayout();
    JPanel grid = new JPanel(layout);
    private final TimePanel timeSync;

	public LFOWidget(Channel channel, LFO lowFrequencyOscillator, int index) {
		super(BoxLayout.PAGE_AXIS);
		this.lfo = lowFrequencyOscillator;
		this.ch = channel;
		this.row = new Row(channel);
		lfoCombo = new LfoCombo(lfo);
		slider = new DoubleSlider(lfo, Min.ordinal(), lfo, Max.ordinal());
		slider.setColors(RANGE);
		slider.setOpaque(true);
		timeSync = new TimePanel(lfo, channel, JudahMidi.getClock());
		msec = new FxKnob(channel, lfo, MSec.ordinal(), "Time", KNOB_C);

		Gui.resize(slider, new Dimension(Size.WIDTH_KNOBS / 2 - 20, Size.STD_HEIGHT));
    	ArrayList<Component> components = row.getControls();
    	components.add(new FxTrigger("Target", lfo, channel));
    	components.add(new FxTrigger("LFO" + index, lfo, channel));
    	components.add(new FxTrigger("min/max", lfo, channel));
    	components.add(timeSync);

    	for (Component c : row.getControls())
			lfoLbls.add(c);
    	add(lfoLbls);

    	GridBagConstraints c = layout.getConstraints(grid);
    	grid.setOpaque(true);
    	grid.add(lfoCombo, c);
    	c.gridx = 1;
    	c.gridwidth = 2;
    	grid.add(slider, c);
    	c.gridx = 3;
    	c.gridwidth = 1;
    	grid.add(msec, c);
		add(grid);
	}

	public final void update() {
		timeSync.update();
		lfoCombo.update();
		slider.update();
		msec.update();
		Color c = ch.isActive(lfo) ? Pastels.getFx(lfo.getClass()) : null;
		wrap.setBackground(c);
		grid.setBackground(c);
		slider.setBackground(c);
	}

	public boolean doKnob(int idx, int data2) {
		switch(idx) {
		case 0 -> {
			LFO.Target target = (LFO.Target)Constants.ratio(data2, LFO.Target.values());
				if (ch.isActive(lfo))
					lfoCombo.midiShow(target);
				else
					lfo.set(Target.ordinal(), target.ordinal());
			}
		case 1 -> lfo.set(Min.ordinal(), data2);
		case 2 -> lfo.set(Max.ordinal(), data2);
		case 3 -> lfo.set(MSec.ordinal(), data2);
		default -> { return false; }
		}
		MainFrame.updateChannel(ch, lfo);
		return true;
	}

	public void bold(boolean yes) {
		for (Component c : row.getControls())
			c.setFont(yes ? Gui.BOLD12 : Gui.FONT11);
	}
}
