package net.judah.song;

import net.judah.JudahZone;

public class BooleanProvider implements Cmdr {
	public static final String TRUE = "true";
	public static final String FALSE = "false";
	public static final String[] keys = {FALSE, TRUE};
	public static BooleanProvider instance = new BooleanProvider();
	
	private BooleanProvider() {
	}
	
	@Override
	public String[] getKeys() {
		return keys;
	}

	@Override
	public String lookup(int value) {
		return value == 1 ? TRUE : FALSE;
	}

	@Override
	public Boolean resolve(String key) {
		return TRUE.equals(key);
	}

	@Override
	public void execute(Param p) {
		if (p.cmd == Cmd.Start) {
			if (resolve(p.val))
				JudahZone.getClock().begin();
			else
				JudahZone.getClock().end();
		}
	}
	
}
