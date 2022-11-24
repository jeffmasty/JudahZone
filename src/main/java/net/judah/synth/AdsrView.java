package net.judah.synth;

import static net.judah.util.Size.STD_HEIGHT;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.util.Slider;

public class AdsrView extends JPanel {
	private static final Dimension SLIDER = new Dimension(70, STD_HEIGHT);

	private final Adsr adsr;
	@Getter private final Slider a = new Slider(0, 200, null);
	@Getter private final Slider d = new Slider(null);
	@Getter private final Slider s = new Slider(null);
	@Getter private final Slider r = new Slider(0, 500, null);
	
	public AdsrView(Adsr input) {
		this.adsr = input;
		JPanel top = new JPanel();
		JPanel bottom = new JPanel();
		for (Slider slider : new Slider[] {
				a, d, s, r}) {
			slider.setPreferredSize(SLIDER);
			slider.setMaximumSize(SLIDER);
		}
		top.add(a);
		top.add(new JLabel("A:D"));
		top.add(d);
		bottom.add(s);
		bottom.add(new JLabel("S:R"));
		bottom.add(r);
		setBorder(BorderFactory.createTitledBorder(SynthPresets.ENVELOPE));
		
		JPanel wrap = new JPanel(new GridLayout(2, 1));
		wrap.add(top);
		wrap.add(bottom);
		add(wrap);
		update();
		a.addChangeListener(e->adsr.setAttackTime(a.getValue()));
		d.addChangeListener(e->adsr.setDecayTime(d.getValue()));
		s.addChangeListener(e-> adsr.setSustainGain(s.getValue() * 0.01f));
		r.addChangeListener(e->adsr.setReleaseTime(r.getValue()));
	}

	public void update() {
		if (a.getValue() != adsr.getAttackTime())
			a.setValue(adsr.getAttackTime());
		if (d.getValue() != adsr.getDecayTime())
			d.setValue(adsr.getDecayTime());
		if (s.getValue() * 0.01f != adsr.getSustainGain())
			s.setValue((int)(adsr.getSustainGain() * 100));
		if (r.getValue() != adsr.getReleaseTime())
			r.setValue(adsr.getReleaseTime());
	}
	
}
