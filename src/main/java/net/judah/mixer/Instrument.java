package net.judah.mixer;

import org.jaudiolibs.jnajack.JackPort;

import judahzone.util.AudioTools;
import judahzone.util.Constants;
import lombok.Getter;
import net.judah.fx.Convolution;
import net.judah.fx.MonoFilter;
import net.judah.fx.MonoFilter.Type;
import net.judahzone.gui.Icons;

@Getter
public class Instrument extends LineIn {

    private String leftSource;
    private String rightSource; // for stereo
    protected JackPort leftPort;
    protected JackPort rightPort;

    private MonoFilter lp;
    private MonoFilter hp;

    /** Mono channel with additional/default HiCut/LoCut */
	public Instrument(String name, String sourcePort, JackPort left, String icon, int lowCut, int hiCut) {
		super(name, Constants.MONO);
		this.icon = Icons.get(icon);
		this.leftPort = left;
		this.leftSource = sourcePort;
		this.rightSource = null;

		hp = new MonoFilter(Type.LoCut, lowCut, 1);
		lp = new MonoFilter(Type.HiCut, hiCut, 1);
		effects.add(hp);
		effects.add(lp);
		setActive(hp, true);
		setActive(lp, true);
	}

	/** Stereo channel */
	public Instrument(String name, String leftSource, String rightSource, String icon) {
		super(name, Constants.STEREO);
		if (icon != null)
			this.icon = Icons.get(icon);
		this.leftSource = leftSource;
		this.rightSource = rightSource;
	}

	@Override public final void processImpl() {
		if (isOnMute())
			return;

		// get raw data
		AudioTools.copy(leftPort.getFloatBuffer(), left);
		if (isStereo)
			AudioTools.copy(rightPort.getFloatBuffer(), right);
		else { // Mono: apply custom filter then Convolution (on or off) splits to stereo
			if (hp != null)
				hp.process(left);
			if (lp != null)
				lp.process(left);
			((Convolution.Mono)IR).monoToStereo(left, right);
		}
		fx();
	}

}
