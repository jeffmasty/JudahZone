package net.judah.mixer;

import java.util.Arrays;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeNotifier;
import net.judah.looper.Recorder;
import net.judah.looper.Sample;
import net.judah.mixer.ChannelGui.Drums;
import net.judah.plugin.BeatBuddy;
import net.judah.util.Console;
import net.judah.util.Constants;

@Data @EqualsAndHashCode(callSuper = true) @Log4j
public class DrumTrack extends Recorder implements TimeListener {

    public static final String NAME = "_drums";
    final LineIn soloTrack;
    TimeNotifier master;

    public DrumTrack(TimeNotifier master, LineIn soloTrack) {
        super(NAME, Type.SOLO, Arrays.asList(new JackPort[] {
                soloTrack.getLeftPort(), soloTrack.getRightPort()
        }), JudahZone.getOutPorts());
        this.master = master;
        master.addListener(this);
        soloTrack.setSolo(true);
        this.soloTrack = soloTrack;
    }

    @Override
    public void update(Property prop, Object value) {
        if (Property.STATUS == prop) {

            log.info(prop + ": " + value);

            if (Status.ACTIVE == value)
                record(true);
            if (Status.TERMINATED == value) {


                record(false);
                // play(true); // already armed
                new Thread() { // concurrent modification
                    @Override public void run() {
                        Constants.sleep(40);
                        master.removeListener(DrumTrack.this);
                        JudahZone.getDrummachine().getQueue().offer(BeatBuddy.PAUSE_MIDI);
                    };
                }.start();
                soloTrack.setSolo(false);
                JudahZone.getDrummachine().play(false);

            }
        }
    }

    public void sync(boolean engage) {
        if (engage) {
            Sample loopA = JudahZone.getLooper().getLoopA();
            loopA.addListener(this);
            master = loopA;
            soloTrack.setSolo(true);
            play(true); // armed
            Console.info("drumtrack sync'd.");
            if (gui != null) ((Drums)gui).armRecord(true);
        }
        else {
            if (master != null)
                master.removeListener(this);
            master = null;
            soloTrack.setSolo(false);
            Console.info("drumtrack disengaged.");
            if (gui != null) ((Drums)gui).armRecord(false);
        }
    }

    public void toggle() {
        if (soloTrack.isSolo()) // disengage drumtrack
            sync(false);
        else { // engage drumtrack
            sync(true);
        }
    }

}
