package net.judah.gui.knobs;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.Size;
import net.judah.samples.Sample;
import net.judah.samples.Sampler;
import net.judah.util.Constants;
import net.judah.widgets.Slider;

public class SamplerKnobs extends KnobPanel {

	private final Sampler sampler;
	@Getter private final JPanel titleBar = new JPanel();
	private final Slider vol; 
	
	public SamplerKnobs(Sampler sampler) {
		super("Samples");
		this.sampler = sampler;
		JPanel loops = new JPanel(new GridLayout(2, 2, 3, 3));
		JPanel oneShots = new JPanel(new GridLayout(2, 2, 3, 3));
		for (int i = 0; i < 4; i++)
			loops.add(sampler.get(i).getPad());
		for (int i = 4; i < 8; i++)
			oneShots.add(sampler.get(i).getPad());

		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(loops);
		add(new JLabel(""));
		add(oneShots);
		vol = new Slider(0, 100, e -> sampler.setVelocity(((Slider)e.getSource()).getValue() * 0.02f), "Sampler Volume");
		Constants.resize(vol, Size.SMALLER_COMBO);
		titleBar.add(vol);
		titleBar.add(new JLabel("Pack"));
		titleBar.add(new JComboBox<String>(new String[] {"Zone", "Jazz", "HipHop"}));
	}

	@Override public Component installing() {
		update();
		return titleBar;
	}
	
	@Override
	public boolean doKnob(int idx, int value) {
		sampler.get(idx).getGain().setVol(value);
		return true;
	}

	public void update(Sample samp) {
		vol.setValue((int) (sampler.getVelocity() * 50));
		for (Sample s : sampler)
			if (s == samp) {
				s.getPad().update();
				repaint();
			}
	}

	
	@Override
	public void update() {
		for (Sample s : sampler)
			s.getPad().update();
				repaint();
	}

	
	
}
