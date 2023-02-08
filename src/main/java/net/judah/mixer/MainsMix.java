package net.judah.mixer;

import java.awt.Color;

public class MainsMix extends MixWidget {

	public MainsMix(Channel channel) {
		super(channel);
		sidecar.add(font(mute));
		sidecar.add(font(fx));
		if (channel.getIcon() == null) 
			title.setText(channel.getName());
		else 
            title.setIcon(channel.getIcon());

	}

	@Override
	protected Color thisUpdate() {
		if (channel.isOnMute())  
			return Color.BLACK;
		if (muted())
			return Color.DARK_GRAY;
		return BLUE; // Mains channel 
	}
	
}
