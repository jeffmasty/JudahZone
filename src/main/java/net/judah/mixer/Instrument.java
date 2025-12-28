package net.judah.mixer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.fx.Convolution;
import net.judah.fx.MonoFilter;
import net.judah.fx.MonoFilter.Type;
import net.judah.gui.Icons;
import net.judah.util.AudioTools;
import net.judah.util.Constants;

@Getter
public class Instrument extends LineIn {

    private String leftSource;
    private String rightSource; // for stereo
    protected JackPort leftPort;
    protected JackPort rightPort;

    private MonoFilter lp;
    private MonoFilter hp;

    /** Mono channel
     * @param j
     * @param i */
	public Instrument(String name, String sourcePort, JackPort left, String icon, int lowCut, int hiCut) {
		super(name, Constants.MONO);
		this.icon = Icons.get(icon);
		this.leftPort = left;
		this.leftSource = sourcePort;
		this.rightSource = null;
		hp = new MonoFilter(Type.LoCut, lowCut, 1);
		lp = new MonoFilter(Type.HiCut, hiCut, 1);
	}

//	public Instrument(String name, String sourcePort, String icon) {
//		this(name, sourcePort, (JackPort)null, icon);
//	}
//
	public Instrument(String name, String leftSource, String rightSource, String icon) {
		super(name, Constants.STEREO);
		if (icon != null)
			this.icon = Icons.get(icon);
		this.leftSource = leftSource;
		this.rightSource = rightSource;
	}

	/** Stereo channel */
	public Instrument(String name, JackPort left, JackPort right, String icon) {
		super(name, true);
		if (icon != null)
			this.icon = Icons.get(icon);
		this.leftPort = left;
		this.rightPort = right;
	}

	@Override
	public final void processImpl() {
		if (isOnMute())
			return;

		// get raw data
		AudioTools.copy(leftPort.getFloatBuffer(), left);
		if (isStereo)
			AudioTools.copy(rightPort.getFloatBuffer(), right);
		else { // Mono: apply custom filter then split to stereo
			if (hp != null)
				hp.process(left);
			if (lp != null)
				lp.process(left);
			((Convolution.Mono)IR).monoToStereo(left, right);  // make Stereo in CabSim
		}
		fx();
	}

}
