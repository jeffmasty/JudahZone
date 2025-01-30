package net.judah.gui.knobs;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.fx.EffectColor;
import net.judah.fx.LFO;
import net.judah.fx.LFO.Target;
import net.judah.gui.MainFrame;
import net.judah.gui.MainFrame.FxChange;
import net.judah.gui.Pastels;
import net.judah.gui.fx.FxTrigger;
import net.judah.gui.fx.Row;
import net.judah.gui.fx.TimePanel;
import net.judah.gui.settable.LfoCombo;
import net.judah.gui.widgets.FxKnob;
import net.judah.mixer.Channel;
import net.judah.util.Constants;

public class LFOWidget extends Box {

	@Getter private final LFO lfo;
	private final Channel ch;

	private final Row row;
	private final LfoCombo lfoCombo;
	private final JPanel wrap = new JPanel();
    private final JPanel lfoLbls =  new JPanel(new GridLayout(1, 4, 0, 1));
    private final JPanel lfoPanel = new JPanel(new GridLayout(1, 4, 0, 0));

    private final ArrayList<Row> labels = new ArrayList<>();

	public LFOWidget(Channel channel, LFO lowFrequencyOscillator, int index) {
		super(BoxLayout.PAGE_AXIS);
		this.lfo = lowFrequencyOscillator;
		this.ch = channel;
		this.row = new Row(channel);
		lfoCombo = new LfoCombo(lfo);
		setOpaque(true);
    	Row lfoLbl = new Row(channel);

    	ArrayList<Component> components = lfoLbl.getControls();
    	components.add(new FxTrigger("Target", lfo, channel));
    	components.add(new FxTrigger("LFO" + index, lfo, channel));
    	components.add(new FxTrigger(" ", lfo, channel));
    	components.add(new TimePanel(lfo, channel));

    	ArrayList<Component> knobs = row.getControls();
    	knobs.add(lfoCombo);
    	knobs.add(new FxKnob(channel, lfo, LFO.Settings.Min.ordinal(), "Min", Pastels.EGGSHELL));
    	knobs.add(new FxKnob(channel, lfo, LFO.Settings.Max.ordinal(), "Max", Pastels.EGGSHELL));
    	knobs.add(new FxKnob(channel, lfo, LFO.Settings.MSec.ordinal(), "Time", Pastels.EGGSHELL));

    	for (Component c : lfoLbl.getControls())
			lfoLbls.add(c);
    	for (int i = 0; i < 4; i++)
    		lfoPanel.add(knobs.get(i));
    	labels.add(lfoLbl);

    	add(lfoLbls);
		add(lfoPanel);

	}

	public final void update() {
		for (Row lbl : labels)
        	lbl.update();
		wrap.setBackground(lfo.isActive() ? EffectColor.get(lfo.getClass()) : null);
		row.update(); // TODO
		lfoCombo.update();
	}

	public boolean doKnob(int idx, int data2) {
		switch(idx) {
		case 0 -> {
			Target target = (Target)Constants.ratio(data2, Target.values());
				if (lfo.isActive())
					lfoCombo.midiShow(target);
				else
					lfo.set(LFO.Settings.Target.ordinal(), target.ordinal());
			}
		case 1 ->  lfo.set(LFO.Settings.Min.ordinal(), data2);
		case 2 -> lfo.set(LFO.Settings.Max.ordinal(), data2);
		case 3 -> lfo.set(LFO.Settings.MSec.ordinal(), data2);
		default -> { return false; }
		}
		MainFrame.update(new FxChange(ch, lfo));
		return true;
	}
}
