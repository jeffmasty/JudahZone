package net.judah.midi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.api.Midi;
import net.judah.beatbox.GMDrum;
import net.judah.plugin.MPK;
import net.judah.song.Edits.Copyable;

/** a networking rule for the Midi Router */
@Data @NoArgsConstructor @AllArgsConstructor
public class MidiRule implements Copyable {

    private Midi fromMidi;
    private Midi toMidi;

    @Override
    public String toString() {
        return MPK.format(getFromMidi()) + " --> " + GMDrum.format(getToMidi());
    }

    @Override
    public MidiRule clone() throws CloneNotSupportedException {
        return new MidiRule(Midi.copy(fromMidi), Midi.copy(toMidi));
    }

}
