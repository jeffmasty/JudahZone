package net.judah.seq.beatbox;

import lombok.Getter;
import net.judah.seq.track.DrumTrack;

@Getter
public class DrumCage {

	final DrumTrack track;
	BeatBox grid;
	DrumMenu menu;

	public DrumCage(DrumTrack t, DrumZone drumz) {
		this.track = t;
		grid = new BeatBox(t, drumz);
		menu = new DrumMenu(grid, drumz);
	}

	public void update() {
		menu.update();
		grid.repaint();
	}

	public void unregister() {
		track.getEditor().removeListener(grid);
	}

}
