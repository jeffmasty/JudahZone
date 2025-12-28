package net.judah.song.cmd;

import java.util.List;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.seq.chords.ChordPlay;
import net.judah.seq.chords.Chords;
import net.judah.seq.chords.Section;

public class ChordCmd implements Cmdr {

	@Getter public static ChordCmd instance = new ChordCmd();
	@Getter public static Player player = new Player();

	@Override public String[] getKeys() {
		List<Section> sections = JudahZone.getInstance().getChords().getSections();
		String[] result = new String[sections.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = sections.get(i).getName();
		return result;
	}
	@Override public Object resolve(String key) {
		List<Section> sections = JudahZone.getInstance().getChords().getSections();
		for (Section s : sections)
			if (key.equals(s.getName()))
				return s;
		return null;
	}
	@Override public void execute(Param p) {
		Chords chords = JudahZone.getInstance().getChords();
		List<Section> sections = chords.getSections();
		if (p.getCmd() != Cmd.Part)
			return;
		for (Section s : sections)
			if (p.getVal().equals(s.getName())) {
				chords.setSection(s, true);
				return;
			}
	}

	static class Player implements Cmdr {
		private final Chords chords = JudahZone.getInstance().getChords();

		@Getter private final String[] keys = {"play", "stop"};
		@Override public Object resolve(String key) {
			return key.equals(keys[0]);
		}

		@Override public void execute(Param p) {
			if (p.getCmd() != Cmd.Chords)
				return;
			boolean active = p.getVal().equals(keys[0]);
			chords.setActive(active);
			Section section = chords.getSection();
			if (active && section != null)
				chords.setChord(section.getChordAt(0));
			ChordPlay.update();
		}

	}



}
