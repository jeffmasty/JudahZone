package net.judah.looper.old;

public enum LoopCommand {


	RECORD("record"), PLAY("play"), STOP("stop"), DUB("dub"), PAUSE("pause") /*TODO*/, CONTINUE("continue") /*TODO*/;

	public final String txt;

	LoopCommand(String name) {
		txt = name;
	}

	@Override
	public String toString() {
		return txt;
	}
}
