package net.judah.mixer;

import java.util.Arrays;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j;
import net.judah.JudahClock;
import net.judah.JudahZone;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeNotifier;
import net.judah.looper.Recorder;
import net.judah.mixer.ChannelGui.Drums;
import net.judah.util.Console;
import net.judah.util.Constants;

@Data @EqualsAndHashCode(callSuper = true) @Log4j
public class DrumTrack extends Recorder implements TimeListener {

    public static final String NAME = "_drums";

    private LineIn soloTrack;
    private TimeNotifier master;

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
                soloTrack.setSolo(false);
                setType(Type.DRUMTRACK);
                new Thread() { // concurrent modification
                    @Override public void run() {
                        Constants.sleep(40);
                        master.removeListener(DrumTrack.this);
                        if (JudahZone.getChannels().getDrums().equals(soloTrack))
                            JudahZone.getDrummachine().play(false);
                        else if (JudahZone.getChannels().getAux2().equals(soloTrack))
                            JudahClock.getInstance().end();
                    };
                }.start();
            }
        }
    }

    public void sync(boolean engage) {
        if (engage) {
            master = JudahZone.getLooper().getLoopA();
            master.addListener(this);
            soloTrack.setSolo(true);
            play(true); // armed
            Console.info("drumtrack sync'd. to " + soloTrack.getName());
            if (gui != null) ((Drums)gui).armRecord(true);
        }
        else {
            if (master != null)
                master.removeListener(this);
            master = null;
            soloTrack.setSolo(false);
            soloTrack = JudahZone.getChannels().getDrums();
            Console.info("drumtrack disengaged.");
            if (gui != null) ((Drums)gui).armRecord(false);
        }
        setType( soloTrack.isSolo() ? Type.SOLO : Type.DRUMTRACK);
    }

    public void toggle() {
        if (soloTrack.isSolo()) // disengage drumtrack
            sync(false);
        else { // engage drumtrack
            sync(true);
        }
    }

}
