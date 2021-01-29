package net.judah.beatbox;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import net.judah.api.Midi;
import net.judah.util.Console;
import net.judah.util.Constants;


public class BeatBox extends ArrayList<DrumTrack> {

    @Getter GMKit kit = GMKit.Standard1;

    @Getter private static final ConcurrentLinkedQueue<ShortMessage>
        queue = new ConcurrentLinkedQueue<>();

    public static final File FOLDER = new File(Constants.ROOT, "patterns");

    public BeatBox() {
        initialize();
    }

    public void initialize() {
        clear();
        for (GMDrum drum : GMDrum.STANDARD_KIT)
            add(new DrumTrack(drum));
    }

    public void process(int step) {
        try {
            for (DrumTrack t : this)
                for (Beat b : t.getBeats())
                    if (b.step == step) {
                        int volume = Math.round(t.getVelocity() * b.getVelocity() * 127f);
                        Midi msg = Midi.create(
                                Midi.NOTE_ON, 9, t.getDrum().getMidi(), volume);
                        queue.add(msg);
                    }

        } catch (Throwable t) {
            Console.warn(step + ": " + t.getMessage(), t);
        }
    }

    public void setKit(GMKit gmKit) {
        this.kit = gmKit;
        getQueue().add(Midi.create(Midi.PROGRAM_CHANGE, 9, gmKit.progChange, 0));
    }

}
