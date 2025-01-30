package net.judah.gui.knobs;

import static net.judah.fx.Compressor.Settings.*;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import net.judah.fx.Compressor;
import net.judah.gui.MainFrame;
import net.judah.gui.MainFrame.FxChange;
import net.judah.gui.Pastels;
import net.judah.gui.fx.Row;
import net.judah.gui.fx.RowLabels;
import net.judah.gui.widgets.FxKnob;
import net.judah.mixer.Channel;
import net.judah.omni.Pair;

public class CompressorWidget extends Box {
	private final Row row;
	private final Channel channel;
	private final Compressor comp;
    private final ArrayList<Row> labels = new ArrayList<>();
    private final JPanel compressorLbls = new JPanel(new GridLayout(1, 4, 0, 1));
    private final JPanel compressor = new JPanel(new GridLayout(1, 4, 0, 0));

	public CompressorWidget(Channel channel) {
		super(BoxLayout.PAGE_AXIS);
		this.channel = channel;
		comp = channel.getCompression();
    	Pair blankComp = new Pair(" ", channel.getCompression());
    	RowLabels compLbl = new RowLabels(channel, new Pair("Compressor", channel.getCompression()), blankComp, blankComp, blankComp);

    	row = new Row(channel);
    	ArrayList<Component> knobs = row.getControls();
    	labels.add(compLbl);

		knobs.add(new FxKnob(channel, comp, Threshold.ordinal(), "THold", Pastels.EGGSHELL));
		knobs.add(new FxKnob(channel, comp, Boost.ordinal(), "Gain", Pastels.EGGSHELL));
    	knobs.add(new FxKnob(channel, comp, Ratio.ordinal(), "Ratio", Pastels.EGGSHELL));
		knobs.add(new FxKnob(channel, comp, Release.ordinal(), "A/R", Pastels.EGGSHELL));

		for (Component c : compLbl.getControls())
			compressorLbls.add(c);
		for (int i = 0; i < knobs.size(); i++)
			compressor.add(knobs.get(i));

		add(compressorLbls);
		add(compressor);

	}

	public boolean doKnob(int idx, int data2) {
    	switch(idx) {
		case 4 -> { // -30 to -1
			comp.set(Threshold.ordinal(), data2);
			compress(2, data2); }
		case 5 -> {
			comp.set(Boost.ordinal(), data2);
			compress(2, data2); }
    	case 6 -> {
			comp.set(Ratio.ordinal(), data2);
			compress(5, data2); }
		case 7 -> {
			comp.set(Release.ordinal(), data2);
			comp.set(Attack.ordinal(), data2);
			compress(2, data2);}
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
		for (Row lbl : labels)
        	lbl.update();
		// wrap.setBackground(lfo.isActive() ? EffectColor.get(lfo.getClass()) : null);
		row.update();

	}
}

