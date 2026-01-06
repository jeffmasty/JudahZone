package net.judah.channel;

import judahzone.util.Constants;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.drumkit.DrumKit;
import net.judah.gui.MainFrame;

/** an live audio channel that can be recorded */
public abstract class LineIn extends Channel {

	@Getter protected boolean muteRecord = true;

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
		for (int i = 0, n = active.size(); i < n; i++)
			active.get(i).process(left, right);
		gain.post(left, right);
	}

}
