package net.judah.util;

import java.awt.Color;
import java.awt.Paint;

import net.judah.beatbox.Beat.Type;

public interface Pastels {
    Color RED = new Color(0xff6e8d);
    Color GREEN = new Color(0x90da6a);
    Color BLUE = new Color(0xbde6fa); //  98dafd // a4b9cb
    Color PINK = new Color(0xf4a2f9);


    static Paint forType(Type type) {
        if (type == Type.NoteOn)
            return PINK;
        if (type == Type.NoteOff)
            return Color.GRAY;
//        if (type == type.Chord)
//            return GREEN;
//        if (type == type.CC)
//            return RED;
        return Color.WHITE;
    }
}

