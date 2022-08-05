package net.judah.tracker.todo;

import lombok.Data;

@Data
public class Chord {

    final Scale scale;
    final Key key;
    final int octave;

    public static Chord fromFile(String raw) {
        String[] split = raw.split("[/]");
        return new Chord(Scale.valueOf(split[0]), Key.valueOf(split[1]), Integer.parseInt(split[2]));
    }

}
