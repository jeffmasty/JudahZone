package net.judah.widgets;


import java.awt.Insets;

import javax.swing.JButton;

import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;
import net.judah.util.Icons;

public class FxButton extends JButton {
	private static final Insets ZERO = new Insets(0, 0, 0, 0);
	
	private FxButton() {
		setIcon(Icons.load("fx.png"));
		setMargin(ZERO);
	}
	
	public FxButton(Channel ch) {
		this();
		addActionListener(e -> MainFrame.setFocus(ch));
	}

	
}
