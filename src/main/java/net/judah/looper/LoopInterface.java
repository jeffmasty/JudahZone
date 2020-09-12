package net.judah.looper;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface LoopInterface {

	void record(boolean active);
	void play(boolean active);
	void undo();
	void redo();
	void clear();
	void channel(int ch, boolean active);

	@RequiredArgsConstructor
	enum CMD {
		RECORD("record loop"), 
		PLAY("play loop"), 
		UNDO("undo recording"), 
		REDO("redo recording"), 
		CLEAR("clear looper"), 
		CHANNEL("record channel");
		@Getter private final String label;
	}

//    public static enum Mode {NEW, ARMED, STARTING, RUNNING, STOPPING, STOPPED};

}
