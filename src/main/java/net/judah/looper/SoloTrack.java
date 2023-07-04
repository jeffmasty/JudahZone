package net.judah.looper;

import org.jaudiolibs.jnajack.JackPort;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.mixer.LineIn;
import net.judah.mixer.Zone;
import net.judah.song.BooleanProvider;
import net.judah.song.Cmd;
import net.judah.song.Cmdr;
import net.judah.song.Param;

@Getter @EqualsAndHashCode(callSuper = true) 
public class SoloTrack extends Loop implements Cmdr {

    public static final String NAME = "D";
    private boolean muteStash = true;
    private LineIn soloTrack;
    
    public SoloTrack(LineIn soloTrack, Looper looper, Zone sources, 
    		JackPort l, JackPort r, Memory mem) {
        super(NAME, "LoopD.png", Type.DRUMTRACK, looper, sources, l, r, mem);
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
        	looper.syncDown(this);
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

	@Override
	public String[] getKeys() {
		return BooleanProvider.keys;
	}

	@Override
	public Boolean resolve(String key) {
		if (BooleanProvider.TRUE.equals(key))
			return true;
		return false;
	}

	@Override
	public void execute(Param p) {
		if (p.cmd == Cmd.Solo) 
			solo(resolve(p.val));
	}

	/**Called from drum pads, if there is no recording and loop is sync'd up 
	 * then start a FREE loop and sync clock's tempo to it */
	public void beatboy() {
		if (looper.getOnDeck().contains(this) == false) return;
		if (isPlaying()) return;
		type = Type.FREE;
		looper.getOnDeck().remove(this);
		record(true);
	}
	
//	@Override
//	protected void endRecord() {
//		super.endRecord();
//		if (type == Type.FREE) {
//			type = Type.DRUMTRACK;
//			clock.syncToLoop();
//			clock.begin();
//		}
//	}
	
}
