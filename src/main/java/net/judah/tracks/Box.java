package net.judah.tracks;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import net.judah.beatbox.Sequence;

public class Box extends ArrayList<ArrayList<Sequence>> {

	@Setter @Getter private ArrayList<Sequence> current = new ArrayList<>();

	public static ArrayList<Sequence> next(boolean forward, ArrayList<ArrayList<Sequence>> beatbox, ArrayList<Sequence> current) {
        int idx = beatbox.indexOf(current);
        if (idx < 0) throw new NullPointerException("current sequence not found");
        if (forward) {
            if (idx == beatbox.size() - 1)
                return beatbox.get(0);
            return beatbox.get(idx + 1);
        }
        if (idx == 0) 
        	return beatbox.get(beatbox.size() - 1);
        return beatbox.get(idx - 1);
	}

	
}
