package net.judah.mixer;

import static net.judah.gui.Gui.font;

import java.awt.Color;

public class MainsMix extends MixWidget {

	public MainsMix(Mains channel) { // TODO HotMic2.0
		super(channel);
		sidecar.add(font(mute));
		sidecar.add(font(fx));
		sidecar.add(font(sync));

		sync.setSelected(channel.isHotMic());
		sync.setText("mic");
		sync.addActionListener(e->channel.hotMic());

		if (channel.getIcon() == null)
			title.setText(channel.getName());
		else
            title.setIcon(channel.getIcon());
		mute.addActionListener(e->channel.setOnMute(!channel.isOnMute()));
	}

	@Override
	protected Color thisUpdate() {
		if (sync.isSelected() != ((Mains)channel).isHotMic())
			sync.setSelected(((Mains)channel).isHotMic());
		sync.setBackground(sync.isSelected() ? RED : null);

		if (mute.isSelected() != channel.isOnMute())
			mute.setSelected(channel.isOnMute());
		if (channel.isOnMute())
			return Color.BLACK;
		if (quiet())
			return Color.GRAY;

		return BLUE; // Mains channel
	}

}
