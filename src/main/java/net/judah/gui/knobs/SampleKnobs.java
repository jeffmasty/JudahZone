package net.judah.gui.knobs;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.fx.Gain;
import net.judah.gui.Gui;
import net.judah.gui.widgets.Slider;
import net.judah.sampler.Sample;
import net.judah.sampler.Sampler;

public class SampleKnobs extends KnobPanel {

	private final Sampler sampler;
	@Getter private final KnobMode knobMode = KnobMode.SAMPLE;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	private final Slider vol;
	private final ArrayList<SamplePad> updates = new ArrayList<>();

	public SampleKnobs(Sampler sampler) {
		this.sampler = sampler;

		JPanel loops = new JPanel(new GridLayout(2, 2, 3, 3));
		for (int i = 0; i < 4; i++)
			updates.add(new SamplePad(sampler.get(i), loops));
		JPanel oneShots = new JPanel(new GridLayout(2, 2, 3, 3));
		for (int i = 4; i < 8; i++)
			updates.add(new SamplePad(sampler.get(i), oneShots));

		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(loops);
		add(new JLabel(""));
		add(oneShots);

		vol = new Slider(0, 100, e -> sampler.setMix(((Slider)e.getSource()).getValue() * 0.01f), "Sampler Volume");
		vol.setValue((int) (sampler.getMix() * 100));
		title.add(Gui.resize(vol, SMALLER_COMBO));
		title.add(new JLabel("Pack"));
		title.add(new JComboBox<String>(new String[] {"Zone", "Jazz", "HipHop"})); // TODO

		update();
		validate();
	}

	public void update(Sample samp) {
		vol.setValue((int) (sampler.getMix() * 100));
		for (SamplePad pad : updates)
			if (pad.sample == samp)
				pad.update();
	}

	private static final int[] KNOB_ORDER = { 0, 1, 4, 5, 2, 3, 6, 7};

	@Override public boolean doKnob(int idx, int value) {
		sampler.get(KNOB_ORDER[idx]).getGain().set(Gain.VOLUME, value);
		return true;
	}

	@Override public void update() {
		updates.forEach(pad ->pad.update());
	}

	@Override public void pad1() {
	}

	@Override public void pad2() {

	}


}
