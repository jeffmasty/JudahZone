package net.judah.looper;

import java.util.Collection;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.mixer.LineIn;

@EqualsAndHashCode(callSuper = true)
public class SoloTrack extends Loop {

    public static final String NAME = "D";
    private boolean muteStash = true;
    @Getter private LineIn soloTrack;
    private boolean soloOn;

    public SoloTrack(LineIn soloTrack, Looper looper, Collection<LineIn> sources) {
        super(NAME, "LoopD.png", looper, sources);
        this.soloTrack = soloTrack;
        drumTrack = true;
    }

    public boolean isSolo() {
    	return soloOn;
    }

    public void solo(boolean engage) {
    	if (engage) {
            muteStash = soloTrack.isMuteRecord();
            soloTrack.setMuteRecord(false);
        }
        else {
            soloTrack.setMuteRecord(muteStash);
        }
    	soloOn = engage;
        MainFrame.update(this);

    }

    /** engage or disengage drumtrack */
    public void toggle() {
    	solo(!soloOn);
    }

    public void setSoloTrack(LineIn input) {
    	soloTrack = input;
    	MainFrame.update(JudahZone.getMixer());
    }


//	/**Called from drum pads, if there is no recording and loop is sync'd up
//	 * then start a FREE loop and sync clock's tempo to it */
//	public void beatboy() {
//		if (looper.getOnDeck().contains(this) == false) return;
//		if (isPlaying()) return;
//		looper.getOnDeck().remove(this);
//		capture(true);
//	}

}
