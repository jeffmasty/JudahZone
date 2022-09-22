package net.judah.drumz;

import java.awt.event.MouseAdapter;

import javax.swing.JButton;

import net.judah.MainFrame;
import net.judah.util.Constants;
import net.judah.util.Knob;
import net.judah.util.Pastels;

public class DrumPad extends Pad {
	
	private final JudahDrumz playa;
	private final JButton fx = new JButton(Constants.FX);
	
	public DrumPad(DrumType type, JudahDrumz playa) {
		super(type);
		this.playa = playa;
		fx.addActionListener(e -> MainFrame.setFocus(getSample()));

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(java.awt.event.MouseEvent e) {
				playa.play(getSample(), true);
			}
		});
		bottom.add(new Knob(null));
		bottom.add(new JButton("Fx"));
		update();
	}

	private DrumSample getSample() {
		return playa.getTracks()[type.ordinal()];
	}

	@Override
	public void update() {
		setBackground(getSample().isActive() ? Pastels.GREEN : Pastels.EGGSHELL);
	}

}
