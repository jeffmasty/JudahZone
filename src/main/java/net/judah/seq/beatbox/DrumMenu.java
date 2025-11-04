package net.judah.seq.beatbox;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import net.judah.gui.Actionable;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.widgets.TrackVol;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.TrackMenu;

public class DrumMenu extends TrackMenu {

	private final DrumZone tab;
	private final DrumTrack track;

	public DrumMenu(BeatBox drumz, DrumZone tab) {

		super(drumz);
		this.tab = tab;
		this.track = (DrumTrack)drumz.getTrack();
		add(new TrackVol(track));
		setMaximumSize(new Dimension(3000, Size.KNOB_HEIGHT));
		file.add(new Actionable("Record On/Off", e->track.setCapture(!track.isCapture())));
		tools.add(new Actionable("Remap", e->MainFrame.setFocus(new RemapView(drumz))));
		tools.add(new Actionable("Clean", e->drumz.clean()));
		tools.add(new Actionable("Condense", e->drumz.condense()));
	}

	@Override public void update() {
		updateCue();
	}

	@Override public void mousePressed(MouseEvent e) {
		if (tab.getCurrent() == track)
			return;
		tab.setCurrent(track); // drums only
		update();
	}
	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mouseReleased(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }

}
