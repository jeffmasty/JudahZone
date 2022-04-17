package net.judah.beatbox;

import lombok.Data;

@Data
public class Beat {
    public static enum Type { NoteOn, NoteOff } // TODO {Chord, CC, Sample, Effects, Tempo }

    private int step;
    private Type type = Type.NoteOn;
    private int velocity = 100;

    public Beat(int step) {
        this.step = step;
    }

    public Beat(int step, Type type) {
        this.step = step;
        this.type = type;
    }
}
