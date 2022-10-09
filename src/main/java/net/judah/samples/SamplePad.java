package net.judah.samples;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import net.judah.JudahZone;
import net.judah.api.ProcessAudio.Type;
import net.judah.util.FxButton;
import net.judah.util.Pastels;

public class SamplePad extends JPanel {
	private final Sample s;
	private final JPanel btns; 
	private final JLabel name;

	// @Getter JButton active = new JButton("on");
	// JButton fx = new JButton("fx");
	public SamplePad(Sample s) {
		this.s = s;
		Color color = s.getType() == Type.ONE_SHOT ? Pastels.MY_GRAY : Pastels.BLUE;
		
		
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				boolean on = !s.isActive();
				JudahZone.getSampler().play(s, on);
			}});
		setBorder(BorderFactory.createSoftBevelBorder(BevelBorder.RAISED, color, color.darker()));
		setLayout(new GridLayout(0, 1));
		setOpaque(true);
		
		name = new JLabel(s.getName(), JLabel.CENTER);
		name.setOpaque(true);
		add(name);
		
		btns = new JPanel();
		// btns.setOpaque(true);
		
		btns.add(new JLabel(" "));
		
		btns.add(new FxButton(s));
		
		add(btns);
	}

	public void update() {
		setBackground(s.isActive() ? Pastels.GREEN : null);
		name.setBackground(getBackground());
		btns.setBackground(getBackground());
		
		repaint();
	}
	
}
