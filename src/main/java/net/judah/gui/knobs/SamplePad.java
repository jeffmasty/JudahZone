package net.judah.gui.knobs;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import net.judah.JudahZone;
import net.judah.api.PlayAudio.Type;
import net.judah.fx.Gain;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.widgets.Knob;
import net.judah.sampler.Sample;

public class SamplePad extends Gui.Opaque {
	public final Sample sample;
	private final JPanel btns;
	private final JLabel name;
	private final Knob vol = new Knob(Pastels.ORANGE);

	public SamplePad(Sample s, JPanel parent) {
		this.sample = s;
		Color color = s.getType() == Type.ONE_SHOT ? Pastels.MY_GRAY : Pastels.BLUE;
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				boolean on = !sample.isPlaying();
				JudahZone.getSampler().play(sample, on);
			}});
		setBorder(BorderFactory.createSoftBevelBorder(BevelBorder.RAISED, color, color.darker()));
		setLayout(new GridLayout(0, 1));
		btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		name = new JLabel(sample.toString(), JLabel.CENTER);
		name.setOpaque(true);
		update();
		vol.addListener(e->sample.getGain().set(Gain.VOLUME, vol.getValue()));

		add(name);
		btns.add(vol);
		//btns.add(new FxButton(s));
		add(btns);
		parent.add(this);
	}

	public void update() {
		setBackground(sample.isPlaying() ? Pastels.GREEN : null);
		name.setBackground(getBackground());
		btns.setBackground(getBackground());
		vol.setValue(sample.getGain().get(Gain.VOLUME));
	}

}
