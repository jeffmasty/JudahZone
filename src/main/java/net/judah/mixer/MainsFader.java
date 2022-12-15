package net.judah.mixer;

import java.awt.Color;

public class MainsFader extends ChannelFader {

	public MainsFader(Channel channel) {
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
		if (channel.getGain().getVol() < 5)
			return Color.DARK_GRAY;
		return MY_GRAY; // Mains channel 
	}
	
}
