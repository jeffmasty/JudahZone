package net.judah.gui.knobs;

import static net.judah.fx.Compressor.Settings.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import net.judah.fx.Compressor;
import net.judah.gui.Gui;
import net.judah.gui.HQ;
import net.judah.gui.MainFrame;
import net.judah.gui.MainFrame.FxChange;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.fx.FxTrigger;
import net.judah.gui.fx.Row;
import net.judah.gui.widgets.FxKnob;
import net.judah.gui.widgets.Slider.FxSlider;
import net.judah.mixer.Channel;

public class CompressorWidget extends JPanel {
	private static final Color KNOB_C = Pastels.EGGSHELL;

	private final Row row;
	private final Row labels;
	private final Channel channel;
	private final Compressor comp;
    private final JPanel compressorLbls = new JPanel(new GridLayout(1, 4, 5, 1));
    private final JPanel compressor = new JPanel(new GridLayout(1, 4, 0, 1));
    private final FxSlider ratio;
    private final FxSlider knee;

	public CompressorWidget(Channel channel) {
		super(new BorderLayout());
		this.channel = channel;
		comp = channel.getCompression();
		ratio = new FxSlider(comp, Ratio.ordinal(), "Ratio");
		knee = new FxSlider(comp, Knee.ordinal(), "Knee");

    	labels = new Row(channel);
    	labels.getControls().add(new FxTrigger("  Compressor", comp, channel));
    	labels.getControls().add(Gui.resize(ratio, Size.MICRO));
    	labels.getControls().add(new FxTrigger("  R/Knee  ", comp, channel));
    	labels.getControls().add(Gui.resize(knee, Size.MICRO));

    	row = new Row(channel);
    	ArrayList<Component> knobs = row.getControls();

		knobs.add(new FxKnob(channel, comp, Threshold.ordinal(), "THold", KNOB_C));
		knobs.add(new FxKnob(channel, comp, Boost.ordinal(), "Gain", KNOB_C, Ratio.ordinal()));
    	knobs.add(new FxKnob(channel, comp, Attack.ordinal(), "Atk", KNOB_C));
		knobs.add(new FxKnob(channel, comp, Release.ordinal(), "Rel", KNOB_C, Knee.ordinal()));

		for (Component c : labels.getControls())
			compressorLbls.add(Gui.wrap(c));
		for (int i = 0; i < knobs.size(); i++)
			compressor.add(knobs.get(i));

		Box inner = new Box(BoxLayout.PAGE_AXIS);
		inner.add(compressorLbls);
		inner.add(compressor);
		add(inner, BorderLayout.CENTER);
	}

	public boolean doKnob(int idx, int data2) {
    	switch(idx) {
		case 4 -> { // -30 to -1
			comp.set(Threshold.ordinal(), data2);
			compress(2, data2); }
		case 5 -> {
			if (HQ.isShift())
				ratio.setValue(data2);
			else {
				comp.set(Boost.ordinal(), data2);
				compress(2, data2);
			}
		}
    	case 6 -> {
			comp.set(Attack.ordinal(), data2);
			compress(2, data2);}
		case 7 -> {
			if (HQ.isShift())
				knee.setValue(data2);
			else {
				comp.set(Release.ordinal(), data2);
				compress(5, data2);
				}
			}
    	default -> { return false; }
    	}
    	MainFrame.update(new FxChange(channel, comp));
    	return true;
	}

	/**turn on/off compressor, run updates
	 * @param threshhold
	 * @param input */
	private void compress(int threshhold, int input) {
		boolean old = comp.isActive();
		comp.setActive(input > threshhold);
		if (old != comp.isActive())
			MainFrame.update(channel);
	}

	public void update() {
		labels.update();
		row.update();
	}
}

