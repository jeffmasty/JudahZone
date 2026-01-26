package net.judah.sampler;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JPanel;

import judahzone.fx.Gain;
import judahzone.gui.Updateable;
import net.judah.gui.MainFrame;

public class SamplePads extends JPanel implements Updateable {

	private final ArrayList<Sample> samples;
	private final ArrayList<SamplePad> updates = new ArrayList<>();

	public SamplePads(Sampler sampler) {
		this.samples = sampler.getSamples();

		JPanel loops = new JPanel(new GridLayout(2, 2, 3, 3));
		for (int i = 0; i < 4; i++)
			updates.add(new SamplePad(samples.get(i), loops, sampler));
		JPanel oneShots = new JPanel(new GridLayout(2, 2, 3, 3));
		for (int i = 4; i < 8; i++)
			updates.add(new SamplePad(samples.get(i), oneShots, sampler));

		setLayout(new GridLayout(1, 2));

		add(loops);
		add(oneShots);

	}

	public void update(Sample samp) {
		for (SamplePad pad : updates)
			if (pad.sample == samp)
				pad.update();
	}

	private static final int[] KNOB_ORDER = { 0, 1, 4, 5, 2, 3, 6, 7};

	public boolean doKnob(int idx, int value) {
		samples.get(KNOB_ORDER[idx]).getGain().set(Gain.VOLUME, value);
		MainFrame.update(this);
		return true;
	}

	@Override public void update() {
		for (SamplePad s : updates)
			s.update();
	}


}
