package net.judah.mixer;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.omni.AudioTools;
import net.judah.omni.Icons;
import net.judah.util.Constants;

@Getter
public class Instrument extends LineIn {

    protected String leftSource;
    protected String rightSource; // for stereo
    protected JackPort leftPort;
    protected JackPort rightPort;

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

	@Override
	public final void process(FloatBuffer outLeft, FloatBuffer outRight) {
		AudioTools.copy(leftPort.getFloatBuffer(), left);
		if (isStereo)
			AudioTools.copy(rightPort.getFloatBuffer(), right);

		fx();

		AudioTools.mix(left, outLeft);
		AudioTools.mix(right, outRight);

	}
}
