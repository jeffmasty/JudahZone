package net.judah.song.cmd;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.song.Overview;

public class SceneCmd implements Cmdr {

	@Getter public static final SceneCmd instance = new SceneCmd();

	private final Overview view = JudahZone.getInstance().getOverview();

	private SceneCmd() { }

	private String[] keys;

	@Override public Object resolve(String key) {
		return view.getSong().getScenes().get(Integer.parseInt(key));
	}

	@Override public void execute(Param p) { /* no-op */ }

	@Override public String[] getKeys() {
		int size = view.getSong().getScenes().size();
		if (keys == null || keys.length != size)
			keys = new String[size];
		for (int i = 0; i < size; i++)
			keys[i] = "" + i;
		return keys;
	}

}
