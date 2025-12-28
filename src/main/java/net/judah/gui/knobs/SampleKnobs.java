package net.judah.gui.knobs;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.sampler.Sample;
import net.judah.sampler.Sampler;

public class SampleKnobs extends KnobPanel {

	private final ArrayList<Sample> samples;
	@Getter private final KnobMode knobMode = KnobMode.Sample;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	private final ArrayList<SamplePad> updates = new ArrayList<>();

	public SampleKnobs(Sampler sampler) {
		this.samples = sampler.getSamples();

		JPanel loops = new JPanel(new GridLayout(2, 2, 3, 3));
		for (int i = 0; i < 4; i++)
			updates.add(new SamplePad(samples.get(i), loops, sampler));
		JPanel oneShots = new JPanel(new GridLayout(2, 2, 3, 3));
		for (int i = 4; i < 8; i++)
			updates.add(new SamplePad(samples.get(i), oneShots, sampler));

		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(loops);
		add(oneShots);
		title.add(new JLabel(""));
		update();
		validate();
	}

	public void update(Sample samp) {
		for (SamplePad pad : updates)
			if (pad.sample == samp)
				pad.update();
	}

	private static final int[] KNOB_ORDER = { 0, 1, 4, 5, 2, 3, 6, 7};

	@Override public boolean doKnob(int idx, int value) {
		samples.get(KNOB_ORDER[idx]).getGain().set(Gain.VOLUME, value);
		MainFrame.update(this);
		return true;
	}

	@Override public void update() {
		for (SamplePad s : updates)
			s.update();
	}


	@Override public void pad1() {
		// open granular
	}

	@Override public void pad2() {
		// pan
	}


}
