package net.judah.mixer;

import java.awt.Color;

import net.judah.looper.SoloTrack;

public class LineMix extends MixWidget {

	private final LineIn in;
	private final SoloTrack soloTrack;
	
	public LineMix(LineIn channel, SoloTrack solo) {
		super(channel);
		this.in = channel;
		this.soloTrack = solo;
		sidecar.add(font(mute));
		sidecar.add(font(fx));
		sidecar.add(font(sync));
		mute.setText("tape");
		mute.setSelected(!channel.isMuteRecord());
		sync.setText("solo");
		sync.addActionListener(e->solo());
		if (channel.getIcon() == null) 
			title.setText(channel.getName());
		else 
            title.setIcon(channel.getIcon());
	}

	@Override
	protected void mute() {
		in.setMuteRecord(!mute.isSelected());
	}

	@Override
	public void updateVolume() {
		super.updateVolume();
		if (in != null && in.isMuteRecord())
			volume.setBackground(BLUE);
	}
	
	@Override
	protected Color thisUpdate() {
		Color bg = MY_GRAY;
		
		if (channel.isOnMute())  // line in/master track 
			bg = Color.BLACK;
		else if (muted())
			bg = Color.DARK_GRAY;
		
		if (soloTrack.isSolo() && soloTrack.getSoloTrack() == in)
			sync.setBackground(YELLOW);
		else 
			sync.setBackground(null);

		sync.setSelected(in == soloTrack.getSoloTrack());
		mute.setSelected(!in.isMuteRecord());
		mute.setBackground(mute.isSelected() ? GREEN : BLUE);
		return bg;
	}

	private void solo() {
		if (sync.isSelected()) {
			soloTrack.setSoloTrack(in);
		}
	}
	
}