package net.judah.tracker.edit;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import net.judah.tracker.Track;
import net.judah.util.Pastels;

@RequiredArgsConstructor
public class CurrentBeat {

	private final Track track;
	
    private ArrayList<BeatLabel> all = new ArrayList<>();
    private BeatLabel previous;

    public ArrayList<BeatLabel> createLabels() {
        all.clear();
        int steps = track.getSteps();
        int stepsPerBeat = track.getDiv();
        int beat = 1;
        for (int x = 0; x < steps; x++) {
            if (x % stepsPerBeat == 0) {
                BeatLabel b = new BeatLabel(track, "  " + beat++, Pastels.BLUE);
                all.add(b);
            }
            else if ((x + 2) % stepsPerBeat == 0 && stepsPerBeat == 4)
                all.add(new BeatLabel(track, "  +"));
            else 
            	all.add(new BeatLabel(track, "  "));
        }
        for (BeatLabel l : all)
        	l.setActive(false);
        return all;
    }

    public void setActive(int step) {
        if (previous != null) previous.setActive(false);
        previous = all.get(step);
        previous.setActive(true);
    }



}
