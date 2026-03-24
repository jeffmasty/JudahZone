package net.judah.channel;

import java.util.concurrent.Callable;

import judahzone.util.Constants;
import lombok.Getter;
import net.judah.gui.MainFrame;

/** an live audio channel that can be recorded */
public abstract class LineIn extends Channel  {


    // preallocated task to avoid per-frame lambda allocation
    @Getter private final Callable<Void> processTask = () -> {
	    process();
	    return null;
	};

	@Getter protected boolean muteRecord = true;

	public LineIn(String name, int channels) {
		this(name, channels == Constants.STEREO);
	}
    public LineIn(String name, boolean stereo) {
    	super(name, stereo);
    }

    public final void setMuteRecord(boolean muteRecord) {
		this.muteRecord = muteRecord;
		MainFrame.updateChannel(Update.MUTE_RECORD, this);
	}

    /** run active stereo effects on this input channel*/
	protected final void fx() {
		hotSwap(); // activate FX
		gain.preamp(left, right);
		for (int i = 0, n = active.size(); i < n; i++)
			active.get(i).process(left, right);
		gain.post(left, right);
	}

}
