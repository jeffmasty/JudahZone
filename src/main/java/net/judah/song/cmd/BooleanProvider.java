package net.judah.song.cmd;

public abstract class BooleanProvider implements Cmdr {
	public static final String TRUE = "true";
	public static final String FALSE = "false";
	public static final String[] keys = {FALSE, TRUE};
//	public static BooleanProvider instance = new BooleanProvider();

//	public BooleanProvider() {
//	}

	@Override
	public String[] getKeys() {
		return keys;
	}

	@Override
	public Boolean resolve(String key) {
		return TRUE.equals(key);
	}

	@Override
	public void execute(Param p) {
	}

}
