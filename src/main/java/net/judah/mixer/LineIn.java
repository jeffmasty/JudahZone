package net.judah.mixer;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.drumkit.DrumKit;
import net.judah.fx.Effect;
import net.judah.gui.MainFrame;
import net.judah.omni.AudioTools;
import net.judah.util.Constants;

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
			MainFrame.update(JudahZone.getDrumMachine());
	}

    /** run active effects on this input channel */
	protected final void fx() {
		if (!isStereo) // split mono
			AudioTools.copy(left, right);
		gain.preamp(left, right);
		for(Effect fx : this)
			if (fx.isActive())
				fx.process(left, right);
		gain.post(left, right);
	}

}
