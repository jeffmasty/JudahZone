package net.judah.looper;

import org.jaudiolibs.jnajack.JackPort;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.mixer.LineIn;
import net.judah.mixer.Zone;

@Getter @EqualsAndHashCode(callSuper = true) 
public class SoloTrack extends Loop {

    public static final String NAME = "D";
    private boolean muteStash = true;
    private LineIn soloTrack;
    
    public SoloTrack(LineIn soloTrack, Looper looper, Zone sources, String icon, JudahClock clock,
    		JackPort l, JackPort r) {
        super(NAME, looper, sources, icon, Type.DRUMTRACK, clock, l, r);
        this.soloTrack = soloTrack;
    }

    public void solo(boolean engage) { 
    	if (engage) {
    		type = Type.SOLO;
            muteStash = soloTrack.isMuteRecord();
            soloTrack.setMuteRecord(false);
        }
        else {
        	type = Type.DRUMTRACK;
        	clock.syncDown(this);
            soloTrack.setMuteRecord(muteStash);
        }
        MainFrame.update(this);

    }

    public boolean isSolo() {
    	return type == Type.SOLO;
    }
    
    /** engage or disengage drumtrack */
    public void toggle() {
    	solo(type != Type.SOLO);
    }

    public void setSoloTrack(LineIn input) {
    	soloTrack = input;
    	MainFrame.update(JudahZone.getMixer());
    }
    
}
