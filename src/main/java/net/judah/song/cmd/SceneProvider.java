package net.judah.song.cmd;

import lombok.Getter;
import net.judah.JudahZone;

public class SceneProvider implements Cmdr {

	@Getter public static final SceneProvider instance = new SceneProvider();
	
	private SceneProvider() { }
	
	static private String[] keys;

	public static void generate() {
		keys = new String[JudahZone.getOverview().getSong().getScenes().size()];
		for (int i = 0; i < keys.length; i++)
			keys[i] = "" + i;
	}
	
	@Override public Object resolve(String key) {
		return JudahZone.getOverview().getSong().getScenes().get(Integer.parseInt(key));
	}

	@Override public void execute(Param p) { /* no-op */ }

	@Override public String[] getKeys() {
		return keys;
	}

}
