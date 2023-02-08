package net.judah.mixer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.gui.Icons;
import net.judah.util.AudioTools;

@Getter
public class Instrument extends LineIn {

    protected String leftSource;
    protected String rightSource; // for stereo

    /** Mono channel */
	public Instrument(String name, String sourcePort, JackPort left, String icon) {
		super(name, false);
		this.icon = Icons.get(icon);
		this.leftPort = left;
		this.leftSource = sourcePort;
		this.rightSource = null;
	}

	public Instrument(String name, String sourcePort, String icon) {
		this(name, sourcePort, (JackPort)null, icon);
	}
	
	public Instrument(String name, String leftSource, String rightSource, String icon) {
		super(name, true);
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

	public void process() {
		if (isStereo) {
			AudioTools.copy(leftPort.getFloatBuffer(), left);
			AudioTools.copy(rightPort.getFloatBuffer(), right);
			processStereoFx(gain.getGain());
		}
		else {
			processFx(leftPort.getFloatBuffer(), 4);
			toStereo(leftPort.getFloatBuffer());
		}
	}
}
