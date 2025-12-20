package net.judah.mixer;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.omni.AudioTools;
import net.judah.omni.Icons;
import net.judah.synth.taco.MonoFilter;
import net.judah.synth.taco.MonoFilter.Type;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@Getter
public class Instrument extends LineIn {

    private String leftSource;
    private String rightSource; // for stereo
    protected JackPort leftPort;
    protected JackPort rightPort;

    private MonoFilter lp;
    private MonoFilter hp;

    /** Mono channel */
	public Instrument(String name, String sourcePort, JackPort left, String icon) {
		super(name, Constants.MONO);
		this.icon = Icons.get(icon);
		this.leftPort = left;
		this.leftSource = sourcePort;
		this.rightSource = null;
	}

	public Instrument(String name, String sourcePort, String icon) {
		this(name, sourcePort, (JackPort)null, icon);
	}

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

	public void setFilter(int lowCut, int hiCut) {
		if (isStereo) {
			RTLogger.warn(this, "Mono Only");
			return;
		}

		if (lowCut <= 0)
			hp = null;
		else
			hp = new MonoFilter(Type.LoCut, lowCut, 1);
		if (hiCut <= 0)
			lp = null;
		else
			lp = new MonoFilter(Type.HiCut, hiCut, 1);
	}

	@Override
	public final void process(FloatBuffer outLeft, FloatBuffer outRight) {
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
			AudioTools.copy(left, right);
		}
		// apply fx
		fx();

		// add to output
		AudioTools.mix(left, outLeft);
		AudioTools.mix(right, outRight);

	}
}
