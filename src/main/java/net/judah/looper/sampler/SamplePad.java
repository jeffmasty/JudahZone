package net.judah.looper.sampler;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.ProcessAudio.Type;
import net.judah.util.Pastels;

public class SamplePad extends JPanel {
	
	private final Sample s;
	@Getter JButton active = new JButton("on");
	JButton fx = new JButton("fx");
	
	
	public SamplePad(Sample s) {
		this.s = s;
		Color color = s.getType() == Type.ONE_SHOT ? Pastels.PURPLE : Pastels.BLUE;
		fx.addActionListener(e -> MainFrame.setFocus(s));
		
		active.addActionListener(e -> {
			boolean on = !s.isActive();
			JudahZone.getSampler().play(JudahZone.getSampler().indexOf(s), on);
			if (on) active.setBackground(Pastels.GREEN);
		});
		active.setBackground(s.isActive() ? Pastels.GREEN : null);
		setBorder(BorderFactory.createSoftBevelBorder(BevelBorder.RAISED, color, color.darker()));
		setLayout(new GridLayout(0, 1));
		active.setOpaque(false);
		
		fx.setOpaque(true);
		
		add(new JLabel(s.getName(), JLabel.CENTER));
		JPanel btns = new JPanel();
		btns.add(active);
		btns.add(fx);
		// btns.add(new JButton("file"));
		add(btns);
	}

	public void update() {
		active.setBackground(s.isActive() ? Pastels.GREEN : null);
	}
	
}
