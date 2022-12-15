package net.judah.looper;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.mixer.LineIn;
import net.judah.mixer.Zone;

@Getter @EqualsAndHashCode(callSuper = true) 
public class SoloTrack extends Loop /* implements TimeListener */{

    public static final String NAME = "D";
    private boolean muteStash = true;
    @Setter private LineIn soloTrack;
    
    public SoloTrack(LineIn soloTrack, Looper looper, Zone sources, String icon, JudahClock clock) {
        super(NAME, looper, sources, icon, Type.DRUMTRACK, clock);
        this.soloTrack = soloTrack;
    }

    public void solo(boolean engage) { 
    	if (engage) {
    		setType(Type.SOLO);
            soloTrack.setSolo(true);
            muteStash = soloTrack.isMuteRecord();
            soloTrack.setMuteRecord(false);
        }
        else {
        	setType(Type.DRUMTRACK);
        	getSync().syncDown();
            soloTrack.setSolo(false);
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

}
