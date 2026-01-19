package net.judah.mixer;

import static judahzone.gui.Gui.font;

import java.awt.Color;

import net.judah.JudahZone;
import net.judah.channel.LineIn;
import net.judah.drumkit.DrumMachine;
import net.judah.looper.SoloTrack;
import net.judah.seq.track.DrumTrack;

public class LineMix extends MixWidget {

	private final LineIn in;
	private final SoloTrack soloTrack;
	private final JudahZone zone;

	public LineMix(LineIn channel, SoloTrack solo, JudahZone judahZone) {
		super(channel);
		this.zone = judahZone;
		this.in = channel;
		this.soloTrack = solo;
		sidecar.add(font(mute));
		sidecar.add(font(fx));
		sidecar.add(font(sync));
		mute.setText("tape");
		sync.setText("solo");
		sync.addActionListener(e->solo());
		mute.addActionListener(e->mute());
	}

	protected void mute() {
		in.setMuteRecord(!in.isMuteRecord());
	}

	@Override
	protected Color thisUpdate() {
		Color bg = MY_GRAY;

		if (channel.isOnMute())  // line in/master track
			bg = Color.BLACK;
		else if (quiet())
			bg = Color.GRAY;


		if (soloTrack != null && soloTrack.isSolo() && soloTrack.getSoloTrack() == in)
			sync.setBackground(YELLOW);
		else
			sync.setBackground(null);

		if (soloTrack != null)
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
			mute.setBackground(in.isMuteRecord() ? null : zone.getLooper().isOnCapture() ? RED : ONTAPE);
		return bg;
	}

	private void solo() {
		if (sync.isSelected()) {
			soloTrack.setSoloTrack(in);
		}
	}

}
