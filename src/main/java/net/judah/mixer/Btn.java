package net.judah.mixer;

import java.awt.event.MouseEvent;

public class Btn extends MixWidget {

	public Btn(Channel channel, String txt) {
		super(channel);
		setText(txt);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

}
