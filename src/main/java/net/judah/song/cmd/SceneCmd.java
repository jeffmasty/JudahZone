package net.judah.song.cmd;

import lombok.Getter;
import net.judah.JudahZone;

public class SceneCmd implements Cmdr {

	@Getter public static final SceneCmd instance = new SceneCmd();

	private SceneCmd() { }

	private String[] keys;

	@Override public Object resolve(String key) {
		return JudahZone.getOverview().getSong().getScenes().get(Integer.parseInt(key));
	}

	@Override public void execute(Param p) { /* no-op */ }

	@Override public String[] getKeys() {
		int size = JudahZone.getOverview().getSong().getScenes().size();
		if (keys == null || keys.length != size)
			keys = new String[size];
		for (int i = 0; i < size; i++)
			keys[i] = "" + i;
		return keys;
	}

}
