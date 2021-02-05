package net.judah.beatbox;

import java.util.ArrayList;
import java.util.Collections;

import lombok.Getter;
import lombok.Setter;
import net.judah.beatbox.BeatBox.Type;

/** All the steps of one step sequencer */
public class Grid extends ArrayList<Sequence> {
    static final int TOTAL_SEQUENCES = GMDrum.Standard.length;

    // TODO probability

    private BeatBox sequencer;
    private KitPanel tracks;

    @Getter @Setter private String kit = "Standard";
    @Getter @Setter private Scale scale = Scale.Major;
    @Getter @Setter private Key key = Key.C;
    @Getter @Setter private int octave = 4;
    @Setter @Getter private int volume = 90;
    @Getter @Setter private boolean mute;

    public Grid(BeatBox sequencer, boolean internal) {
        this.sequencer = sequencer;
    }

    public Grid(BeatBox sequencer) {
        this.sequencer = sequencer;
        initialize();
    }

    public void initialize() {
        clear();
        if (sequencer.getType() == Type.Melodic) {
            int rollover = 0;
            int scaleIdx = 0;
            for (int i = 0; i < TOTAL_SEQUENCES; i++) {
                add(new Sequence(new MidiBase((octave + rollover) * 12 +
                        sum(scaleIdx) + key.ordinal())));
                scaleIdx++;
                if (scaleIdx == scale.getStep().size()) {
                    scaleIdx = 0;
                    rollover++;
                }
            }
            Collections.reverse(this);
        }
        else // drums
            for (GMDrum drum : GMDrum.KITS.get(kit))
                add(new Sequence(new MidiBase(drum)));
    }

    private int sum(int scaleStep) {
        int result = 0;
        for (int i = 0; i <= scaleStep; i++) {
            if (i == 0) continue;
            result += scale.getStep().get(i-1);
        }
        return result;
    }

    public void redoTracks() {
        int scaleIdx = 0;
        int rollover = 0;
        synchronized(this) {

            Collections.reverse(this);

            for (int i = 0; i < this.size(); i++) {
                get(i).setReference(new MidiBase( (octave + rollover) * 12 +
                        sum(scaleIdx) + key.ordinal()));
                scaleIdx++;
                if (scaleIdx == scale.getStep().size()) {
                    scaleIdx = 0;
                    rollover++;
                }
            }
            Collections.reverse(this);
        }

        if (tracks != null && tracks instanceof PianoTracks)
            ((PianoTracks)tracks).reLabel();
    }

    public KitPanel createTracks() {
        tracks = (sequencer.getType() == Type.Drums)
            ? new DrumTracks(this)
            : new PianoTracks(this);
        return tracks;
    }

}
