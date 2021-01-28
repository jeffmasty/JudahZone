package net.judah.beatbox;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CurrentBeat {

    private final int steps;
    private final int stepsPerBeat;
    ArrayList<BeatLabel> all = new ArrayList<>();

    BeatLabel previous;

    public ArrayList<BeatLabel> getLabels() {
        int beat = 1;
        for (int x = 0; x < steps; x++)
            if (x % stepsPerBeat == 0)
                all.add(new BeatLabel(" " + beat++));
            else if ((x + 2) % stepsPerBeat == 0 && stepsPerBeat == 4)
                all.add(new BeatLabel(" +"));
            else all.add(new BeatLabel("  "));
        return all;
    }

    public void setActive(int step) {
        if (previous != null) previous.setActive(false);
        previous = all.get(step);
        previous.setActive(true);
    }



}
