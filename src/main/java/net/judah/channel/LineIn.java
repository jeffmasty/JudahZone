package net.judah.channel;

import judahzone.util.Constants;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.drumkit.DrumKit;
import net.judah.gui.MainFrame;

/** an live audio channel that can be recorded */
@Getter
public abstract class LineIn extends Channel {

	protected boolean muteRecord = true;

	public LineIn(String name, int channels) {
		this(name, channels == Constants.STEREO);
	}
    public LineIn(String name, boolean stereo) {
    	super(name, stereo);
    }

    public final void setMuteRecord(boolean muteRecord) {
		this.muteRecord = muteRecord;
		MainFrame.update(this);
		if (this instanceof DrumKit)
			MainFrame.update(JudahZone.getInstance().getDrumMachine());
	}

    /** run active stereo effects on this input channel*/
	protected final void fx() {
		gain.preamp(left, right);
		active.forEach(fx -> fx.process(left, right));
		gain.post(left, right);
	}

}
