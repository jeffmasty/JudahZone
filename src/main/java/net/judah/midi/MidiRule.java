package net.judah.midi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.api.Midi;
import net.judah.controllers.MPKTools;
import net.judah.sequencer.editor.Edits.Copyable;
import net.judah.tracker.GMDrum;

/** a networking rule for the Midi Router */
@Data @NoArgsConstructor @AllArgsConstructor
public class MidiRule implements Copyable {

    private Midi fromMidi;
    private Midi toMidi;

    @Override
    public String toString() {
        return MPKTools.format(getFromMidi()) + " --> " + GMDrum.format(getToMidi());
    }

    @Override
    public MidiRule clone() throws CloneNotSupportedException {
        return new MidiRule(Midi.copy(fromMidi), Midi.copy(toMidi));
    }

}
