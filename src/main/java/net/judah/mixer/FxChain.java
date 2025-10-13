package net.judah.mixer;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import lombok.Getter;
import net.judah.fx.Effect;
import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.util.Constants;

/** An input or output effects bus for mono or stereo audio */
@Getter
public abstract class FxChain extends ArrayList<Effect> {
	protected static final int N_FRAMES = Constants.bufSize();
	protected static final int S_RATE = Constants.sampleRate();
	protected final FloatBuffer left = FloatBuffer.wrap(new float[N_FRAMES]);
    protected final FloatBuffer right = FloatBuffer.wrap(new float[N_FRAMES]);

	protected final String name;
	protected ImageIcon icon;
	protected final boolean isStereo;
	protected boolean onMute;
	protected final Gain gain = new Gain();

	public FxChain(String name, int numChannels) {
		this(name, numChannels == Constants.STEREO);
	}

	public FxChain(String name, boolean stereo) {
		isStereo = stereo;
		this.name = name;
	}

	abstract public void process(FloatBuffer left, FloatBuffer right);

	public void reset() {
		forEach(fx->fx.setActive(false));
		MainFrame.update(this);
	}

	/**@return 0 to 100*/
	public final int getVolume() {
		return gain.get(Gain.VOLUME);
	}

	/**@return 0 to 100*/
	public final int getPan() {
		return gain.get(Gain.PAN);
	}

	@Override public String toString() { return name; }

}
