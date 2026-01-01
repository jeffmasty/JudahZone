package net.judah.gui.knobs;

import static net.judah.fx.Compressor.Settings.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.util.RTLogger;
import net.judah.channel.Channel;
import net.judah.fx.Compressor;
import net.judah.gui.HQ;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.fx.FxTrigger;
import net.judah.gui.fx.Row;
import net.judah.gui.widgets.FxKnob;
import net.judah.gui.widgets.Slider.FxSlider;

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
		Gui.resize(ratio, Size.MICRO);
		Gui.resize(knee, Size.MICRO);

    	labels = new Row(channel);
    	labels.add(new FxTrigger("  Compressor", comp, channel));
    	labels.add(ratio);
    	labels.add(new FxTrigger("  R/Knee  ", comp, channel));
    	labels.add(knee);

    	row = new Row(channel);

		row.add(new FxKnob(channel, comp, Threshold.ordinal(), "THold", KNOB_C));
		row.add(new FxKnob(channel, comp, Boost.ordinal(), "Gain", KNOB_C, Ratio.ordinal()));
    	row.add(new FxKnob(channel, comp, Attack.ordinal(), "Atk", KNOB_C));
		row.add(new FxKnob(channel, comp, Release.ordinal(), "Rel", KNOB_C, Knee.ordinal()));

		for (Component c : labels.list())
			compressorLbls.add(Gui.wrap(c));

		for (Component c : row.list())
			compressor.add(c);

		Box inner = new Box(BoxLayout.Y_AXIS);
		inner.add(compressorLbls);
		inner.add(compressor);
		add(inner, BorderLayout.CENTER);
	}

	public boolean doKnob(int idx, int data2) {
    	switch(idx) {
		case 4 -> { // -30 to -1
			if (HQ.isShift()) { RTLogger.log(this, "hi there");
				return false; // fwd to cabsim
			}
			comp.set(Threshold.ordinal(), data2);
			compress(2, data2); }
		case 5 -> {
    		if (HQ.isShift())
    			return false; // fwd to Cabsim
			else {
				comp.set(Boost.ordinal(), data2);
				compress(2, data2);
			}
		}
    	case 6 -> {
			if (HQ.isShift())
				ratio.setValue(data2);
			else {
				comp.set(Attack.ordinal(), data2);
				compress(2, data2);}
    	}
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
    	MainFrame.updateFx(channel, comp);
    	return true;
	}

	/**turn on/off compressor, run updates
	 * @param threshhold
	 * @param input */
	private void compress(int threshhold, int input) {
		channel.setActive(comp, input > threshhold);
	}

	public void update() {
		labels.update();
		row.update();
	}
}

