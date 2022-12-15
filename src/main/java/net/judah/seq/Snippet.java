package net.judah.seq;

import java.util.ArrayDeque;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** chunk of a midi track */
@Data @NoArgsConstructor @EqualsAndHashCode(callSuper=true) 
public class Snippet extends ArrayDeque<Note>{
	public long startref;
	public Bar one, two, three;
	
}
