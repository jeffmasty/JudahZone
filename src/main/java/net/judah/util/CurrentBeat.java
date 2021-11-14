package net.judah.util;

import java.util.ArrayList;

import net.judah.clock.JudahClock;

public class CurrentBeat {

    private final JudahClock clock = JudahClock.getInstance();
    private ArrayList<BeatLabel> all = new ArrayList<>();
    private BeatLabel previous;

    public ArrayList<BeatLabel> createLabels() {
        all.clear();
        int steps = clock.getSteps();
        int stepsPerBeat = clock.getSubdivision();
        int beat = 1;
        for (int x = 0; x < steps; x++)
            if (x % stepsPerBeat == 0) {
                BeatLabel b = new BeatLabel("  " + beat++);
                b.setBackground(Pastels.BLUE);
                all.add(b);
            }
            else if ((x + 2) % stepsPerBeat == 0 && stepsPerBeat == 4)
                all.add(new BeatLabel("  +"));
            else all.add(new BeatLabel("  "));
        return all;
    }

    public void setActive(int step) {
        if (previous != null) previous.setActive(false);
        previous = all.get(step);
        previous.setActive(true);
    }



}
