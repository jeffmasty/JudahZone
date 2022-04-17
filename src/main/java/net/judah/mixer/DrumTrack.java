package net.judah.mixer;

import java.util.Arrays;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Notification;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeNotifier;
import net.judah.clock.JudahClock;
import net.judah.looper.Recorder;
import net.judah.util.RTLogger;

@Data @EqualsAndHashCode(callSuper = true) 
public class DrumTrack extends Recorder implements TimeListener {

    public static final String NAME = "_drums";

    private boolean muteStash = true;
    
    private LineIn soloTrack;
    private TimeNotifier master;

    public DrumTrack(TimeNotifier master, LineIn soloTrack) {
        super(NAME, Type.SOLO, Arrays.asList(new JackPort[] {
                soloTrack.getLeftPort(), soloTrack.getRightPort()
        }), JudahZone.getOutPorts());
        this.master = master;
        this.soloTrack = soloTrack;
    }

    @Override
    public void update(Notification.Property prop, Object value) {
        if (Notification.Property.STATUS == prop) {
            if (Status.ACTIVE == value)
                record(true);
            else if (Status.TERMINATED == value) {
                setType(Type.DRUMTRACK);
                if (JudahZone.getChannels().getDrums().equals(soloTrack))
                	JudahClock.getInstance().end();
                sync(false);
            }
        }
    }

    public void sync(boolean engage) {
    	sync = engage;
    	if (engage) {
            master = JudahZone.getLooper().getLoopA();
            master.addListener(this);
            soloTrack.setSolo(true);
            play(true); // armed
            LineIn drums = JudahZone.getChannels().getDrums();
            muteStash = drums.isMuteRecord();
            drums.setMuteRecord(false);
            RTLogger.log(this, "drumtrack sync'd. to " + soloTrack.getName());
        }
        else {
            if (master != null)
                master.removeListener(this);
            master = null;
            soloTrack.setSolo(false);
            soloTrack = JudahZone.getChannels().getDrums();
            JudahZone.getChannels().getDrums().setMuteRecord(muteStash);
            RTLogger.log(this, "drumtrack disengaged.");
        }
        setType(soloTrack.isSolo() ? Type.SOLO : Type.DRUMTRACK);
        MainFrame.update(this);

    }

    /** engage or disengage drumtrack */
    public void toggle() {
    	sync(!isSync());
    }

}
