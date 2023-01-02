package net.judah.song;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class Param {

	public static enum Type {
		CLOCK, LOOP, CH
	}
	
	Cmd cmd;
	int val;
	
}
