package net.judah.beatbox;

import static net.judah.beatbox.GMDrum.*;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import net.judah.api.Midi;
import net.judah.util.Console;

public class BeatBox extends ArrayList<DrumTrack> {

    private GMKit kit = GMKit.Standard1;

    @Getter private static final GMDrum[] STANDARD_KIT = new GMDrum[] {
            BassDrum, AcousticSnare, SideStick, HandClap,
            PedalHiHat, ClosedHiHat, OpenHiHat, SplashCymbal,
            HighTom, LowMidTom, Shaker, Cabasa,
            HiBongo, LowBongo, OpenHiConga, HighTimbale,
            HiWoodBlock, OpenCuica, RideCymbal2, Vibraslap };

    @Getter private static final ConcurrentLinkedQueue<ShortMessage> queue = new ConcurrentLinkedQueue<>();

    public BeatBox() {
        initialize();
    }

    public void initialize() {
        clear();
        for (GMDrum drum : STANDARD_KIT)
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
