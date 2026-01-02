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

import judahzone.api.PlayAudio.Type;
import judahzone.fx.Gain;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import lombok.Getter;
import net.judah.gui.widgets.Knob;
import net.judah.sampler.Sample;
import net.judah.sampler.Sampler;

public class SamplePad extends Gui.Opaque {
	public final Sample sample;
	private final JPanel btns;
	private final JLabel name;
	@Getter private final Knob knob = new Knob(Pastels.ORANGE);

	public SamplePad(Sample s, JPanel parent, Sampler sampler) {
		this.sample = s;
		Color color = s.getType() == Type.ONE_SHOT ? Pastels.MY_GRAY : Pastels.BLUE;
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				boolean on = !sample.isPlaying();
				sampler.play(sample, on);
			}});
		setBorder(BorderFactory.createSoftBevelBorder(BevelBorder.RAISED, color, color.darker()));
		setLayout(new GridLayout(0, 1));
		btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		name = new JLabel(sample.toString(), JLabel.CENTER);
		name.setOpaque(true);
		update();
		knob.addListener(e->sample.getGain().set(Gain.VOLUME, knob.getValue()));

		add(name);
		btns.add(knob);
		//btns.add(new FxButton(s));
		add(btns);
		parent.add(this);
	}

	public void update() {
		setBackground(sample.isPlaying() ? Pastels.GREEN : null);
		name.setBackground(getBackground());
		btns.setBackground(getBackground());
		knob.setValue(sample.getGain().get(Gain.VOLUME));
	}

}
