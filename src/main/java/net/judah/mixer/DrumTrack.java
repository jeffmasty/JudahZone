package net.judah.mixer;

import java.util.Arrays;
import java.util.List;

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
import net.judah.looper.Loop;
import net.judah.util.RTLogger;

@Data @EqualsAndHashCode(callSuper = true) 
public class DrumTrack extends Loop implements TimeListener {

    public static final String NAME = "_drums";

    private boolean muteStash = true;
    protected final List<JackPort> inputPorts;
    private LineIn soloTrack;
    private TimeNotifier master;

    public DrumTrack(TimeNotifier master, LineIn soloTrack) {
        super(NAME);
        this.master = master;
        this.soloTrack = soloTrack;
        inputPorts = Arrays.asList(new JackPort[] {
                soloTrack.getLeftPort(), soloTrack.getRightPort()});
    }

    @Override
    public void update(Notification.Property prop, Object value) {
        if (Notification.Property.STATUS == prop) {
            if (Status.ACTIVE == value)
                record(true);
            else if (Status.TERMINATED == value) {
                setType(Type.DRUMTRACK);
                if (JudahZone.getChannels().getCalf().equals(soloTrack))
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
            LineIn drums = JudahZone.getChannels().getCalf();
            muteStash = drums.isMuteRecord();
            drums.setMuteRecord(false);
            RTLogger.log(this, "drumtrack sync'd. to " + soloTrack.getName());
        }
        else {
            if (master != null)
                master.removeListener(this);
            master = null;
            soloTrack.setSolo(false);
            soloTrack = JudahZone.getChannels().getCalf();
            soloTrack.setMuteRecord(muteStash);
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
