package net.judah.beatbox;

import javax.sound.midi.ShortMessage;

import lombok.Data;
//import lombok.RequiredArgsConstructor;

@Data //@RequiredArgsConstructor
public class MidiBase {

    private final int command;
    private final int data1;
    private GMDrum drum; // drum grid only

    public MidiBase(int data1) {
        this.command = ShortMessage.NOTE_ON;
        this.data1 = data1;
    }

    public MidiBase(GMDrum drum) {
        this(drum.getMidi());
        this.drum = drum;
    }

    @Override
    public String toString() {
        if (drum == null)
            return Key.values()[data1 % 12].name() + "  " + data1;
        return drum.getDisplay() + "." + data1;
    }
}
