package net.judah.mixer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Notification;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.util.RTLogger;

@Data @EqualsAndHashCode(callSuper = true) 
public class SoloTrack extends Loop implements TimeListener {

    public static final String NAME = "D";
    private boolean muteStash = true;
    private Instrument soloTrack;

    public SoloTrack(Instrument soloTrack, Looper looper) {
        super(NAME, looper);
        this.soloTrack = soloTrack;
    }

	@Override
    public void update(Notification.Property prop, Object value) {
        if (Notification.Property.STATUS == prop) {
            if (Status.ACTIVE == value)
                record(true);
            else if (Status.TERMINATED == value) {
                record(false);
            	setType(Type.DRUMTRACK);
                if (JudahZone.getChannels().getCalf().equals(soloTrack))
                	JudahClock.getInstance().end();
                solo(false);
	        	getRecording().trim();
            }
        }
    }

    public void solo(boolean engage) {
    	armed = engage;
    	if (engage) {
    		for (Loop x : looper) 
    			x.addListener(this);
            soloTrack.setSolo(true);
            muteStash = soloTrack.isMuteRecord();
            soloTrack.setMuteRecord(false);
            RTLogger.log(this, "drumtrack sync'd. to " + soloTrack.getName());
        }
        else {
        	for (Loop x : looper)
        		x.removeListener(this);
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
    	solo(!isArmed());
    }

}
