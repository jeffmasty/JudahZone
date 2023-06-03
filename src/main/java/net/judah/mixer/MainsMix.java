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
		mute.addActionListener(e->channel.setOnMute(!channel.isOnMute()));
	}

	@Override
	protected Color thisUpdate() {
		if (channel.isOnMute())  
			return Color.BLACK;
		if (quiet())
			return Color.DARK_GRAY;
		
		if (mute.isSelected() != channel.isOnMute())
			mute.setSelected(channel.isOnMute());
		
		return BLUE; // Mains channel 
	}
	
}
