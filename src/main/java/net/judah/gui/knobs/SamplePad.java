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
import net.judah.api.ProcessAudio.Type;
import net.judah.drumkit.Sample;
import net.judah.gui.Pastels;
import net.judah.gui.widgets.FxButton;
import net.judah.gui.widgets.Knob;

public class SamplePad extends JPanel {
	private final Sample sample;
	private final JPanel btns; 
	private final JLabel name;
	private final Knob vol = new Knob(Pastels.ORANGE);
	
	public SamplePad(Sample s) {
		this.sample = s;
		Color color = s.getType() == Type.ONE_SHOT ? Pastels.MY_GRAY : Pastels.BLUE;
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				boolean on = !s.isActive();
				JudahZone.getSampler().play(s, on);
			}});
		setBorder(BorderFactory.createSoftBevelBorder(BevelBorder.RAISED, color, color.darker()));
		setLayout(new GridLayout(0, 1));
		setOpaque(true);
		btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		name = new JLabel(s.getName(), JLabel.CENTER);
		name.setOpaque(true);
		update();
		vol.addListener(e->sample.getGain().setVol(vol.getValue()));

		add(name);
		btns.add(vol);
		btns.add(new FxButton(s));
		add(btns);
	}

	public void update() {
		setBackground(sample.isActive() ? Pastels.GREEN : null);
		name.setBackground(getBackground());
		btns.setBackground(getBackground());
		vol.setValue(sample.getVolume());
		repaint();
	}
	
}
