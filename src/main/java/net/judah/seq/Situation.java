package net.judah.seq;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor @ToString @Getter
public class Situation {

	public static final int PREVIOUS = 0;
	public static final int CURRENT = 1;
	public static final int NEXT = 2;
	public static final int AFTERNEXT = 3;
	
	public int previous;
	public int current;
	public int next;
	public int afterNext;
	
	public int get(int idx) {
		switch (idx) {
			case PREVIOUS: return previous;
			case CURRENT: return current;
			case NEXT: return next;
			case AFTERNEXT: return afterNext;
		}
		return 0;
	}
	
	
}
