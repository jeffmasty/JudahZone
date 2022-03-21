package net.judah.mixer;

import java.awt.event.MouseEvent;

import net.judah.util.Icons;
import net.judah.util.RTLogger;


public class Menu extends MixWidget {

	public Menu(Channel ch) {
		super(ch);
		setIcon(Icons.load("Gear.png"));
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		RTLogger.log(this, channel.getName() + "'s menu is under construction.");
	}

	public void update() {
		// subclass
	}
}
