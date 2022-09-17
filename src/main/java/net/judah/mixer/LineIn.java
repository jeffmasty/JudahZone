package net.judah.mixer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.MainFrame;
import net.judah.util.GuitarTuner;

@Getter
public abstract class LineIn extends Channel {
    protected boolean muteRecord;
    @Setter protected boolean solo;
    protected LatchEfx latchEfx = new LatchEfx(this);
    /** set to <code>null</code> for no processing */
    @Setter protected GuitarTuner tuner;
    @Setter protected JackPort sync;
    
    public LineIn(String name, boolean stereo) {
    	super(name, stereo);
    }
    
    public abstract void process();
    
    public void setMuteRecord(boolean muteRecord) {
		this.muteRecord = muteRecord;
		MainFrame.update(this);
	}


    
}
