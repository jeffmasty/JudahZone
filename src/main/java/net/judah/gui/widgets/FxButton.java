package net.judah.gui.widgets;


import javax.swing.Icon;
import javax.swing.JButton;

import judahzone.gui.Gui;
import judahzone.gui.Icons;
import net.judah.channel.Channel;
import net.judah.gui.MainFrame;

public class FxButton extends JButton {

	public static Icon icon() {
		return Icons.get("fx.png");
	}

	public FxButton(Channel ch) {
		setIcon(icon());
		setMargin(Gui.BTN_ZERO);
		addActionListener(e -> MainFrame.setFocus(ch));
	}

}
