package net.judah.mixer;

import static net.judah.gui.Gui.font;

import java.awt.Color;

import net.judah.JudahZone;
import net.judah.drumkit.DrumMachine;
import net.judah.looper.Looper;
import net.judah.looper.SoloTrack;
import net.judah.seq.track.DrumTrack;

public class LineMix extends MixWidget {

	private final LineIn in;
	private final SoloTrack soloTrack;

	public LineMix(LineIn channel, Looper looper) {
		super(channel, looper);
		this.in = channel;
		this.soloTrack = looper.getSoloTrack();
		sidecar.add(font(mute));
		sidecar.add(font(fx));
		sidecar.add(font(sync));
		mute.setText("tape");
		sync.setText("solo");
		sync.addActionListener(e->solo());
		if (channel.getIcon() == null)
			title.setText(channel.getName());
		else
            title.setIcon(channel.getIcon());
		mute.addActionListener(e->mute());
	}

	protected void mute() {
		in.setMuteRecord(!in.isMuteRecord());
	}

	@Override
	public void updateVolume() {
		super.updateVolume();
		if (!in.isMuteRecord() && in != JudahZone.getDrumMachine())
			fader.setBackground(ONTAPE);
	}

	@Override
	protected Color thisUpdate() {
		Color bg = MY_GRAY;

		if (channel.isOnMute())  // line in/master track
			bg = Color.BLACK;
		else if (quiet())
			bg = Color.GRAY;

		if (soloTrack.isSolo() && soloTrack.getSoloTrack() == in)
			sync.setBackground(YELLOW);
		else
			sync.setBackground(null);

		sync.setSelected(in == soloTrack.getSoloTrack());
		if (in instanceof DrumMachine drumz) {
			if (drumz.isMuteRecord())
				mute.setBackground(null);
			else for (DrumTrack t : drumz.getTracks())
				if (!t.getKit().isMuteRecord()) {
					mute.setBackground(RED);
					return bg;
				}
			mute.setBackground(null);
		}
		else
			mute.setBackground(in.isMuteRecord() ? null : ONTAPE);

		return bg;
	}

	private void solo() {
		if (sync.isSelected()) {
			soloTrack.setSoloTrack(in);
		}
	}

}
