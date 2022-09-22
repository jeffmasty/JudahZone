package net.judah.mixer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Notification;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.drumz.JudahDrumz;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.synth.JudahSynth;
import net.judah.util.RTLogger;

@Data @EqualsAndHashCode(callSuper = true) 
public class SoloTrack extends Loop implements TimeListener {

    public static final String NAME = "D";
    private boolean muteStash = true;
    private LineIn soloTrack;

    public SoloTrack(LineIn soloTrack, Looper looper, Channels instruments, JudahSynth[] synth, JudahDrumz[] beats) {
        super(NAME, looper, instruments, synth, beats);
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
                if (JudahZone.getInstruments().getCalf().equals(soloTrack))
                	JudahZone.getClock().end();
                if (JudahZone.getBeats().equals(soloTrack))
                	JudahZone.getClock().end();
                solo(false);
                if (hasRecording())
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
            soloTrack = JudahZone.getInstruments().getCalf();
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
