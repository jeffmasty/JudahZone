package net.judah.seq.beatbox;

import lombok.Getter;
import net.judah.seq.track.DrumTrack;

@Getter
public class DrumCage {

	final DrumTrack track;
	final BeatBox grid;
	final DrumMenu menu;

	public DrumCage(DrumTrack t, DrumZone drumz) {
		this.track = t;
		grid = new BeatBox(t, drumz);
		menu = new DrumMenu(grid, drumz);
	}

	public void update() {
		menu.update();
		grid.repaint();
	}

}
