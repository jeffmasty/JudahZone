package net.judah.tracker;

import java.util.ArrayList;

import lombok.Getter;

/* see also https://github.com/sangervu/MusicScales/blob/master/MusicScales/src/musicscales/MusicScales.java */
public enum Scale {

    Major(2, 2, 1, 2, 2, 2, 1),
    Dom7(2, 2, 1, 2, 2, 1, 2),
    Min7(2, 1, 2, 2, 2, 1, 2),
    TeChNo(2, 1, 2, 2, 1, 3, 1), // harmonic minor
    Penta(2, 2, 3, 2, 3),
    Blues(3, 2, 1, 1, 3, 2),
    Bebop(2, 2, 1, 2, 1, 1, 2, 1),
    Chromatic(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    ;

    @Getter private final ArrayList<Integer> step = new ArrayList<>();

    Scale(int... steps) {
        for (int i : steps)
            step.add(i);
    }

}
