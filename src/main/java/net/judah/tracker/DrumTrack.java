package net.judah.tracker;

import java.util.ArrayList;

import lombok.Getter;
import net.judah.drumz.DrumKit;
import net.judah.midi.JudahClock;
import net.judah.tracker.edit.DrumEdit;

public class DrumTrack extends Track {

	@Getter private final DrumKit kit;
	@Getter private final ArrayList<Float> volume = new ArrayList<>();
	
	public DrumTrack(JudahClock clock, String name, DrumKit drums, DrumTracks tracker) {
		super(name, tracker, drums, 9);
		this.kit = drums;
		edit = new DrumEdit(this);

	}

}
