package net.judah.mixer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.util.Icons;

/**JudahZone mixer Channels come with built-in compression, reverb and gain */
@Getter
public class Instrument extends LineIn {

    protected final String leftSource;
    protected final String rightSource; // for stereo

    /** Mono channel */
	public Instrument(String name, String sourcePort, JackPort left, String icon) {
		super(name, false);
		this.icon = Icons.load(icon);
		this.leftPort = left;
		this.leftSource = sourcePort;
		this.rightSource = null;
	}

	public Instrument(String name, String sourcePort, String icon) {
		this(name, sourcePort, (JackPort)null, icon);
	}
	
	public Instrument(String name, String leftSource, String rightSource, String icon) {
		this(name, leftSource, rightSource, null, null, icon);
	}
	
	/** Stereo channel */
	public Instrument(String name, String leftSource, String rightSource, JackPort left, JackPort right, String icon) {
		super(name, true);
		if (icon != null)
			this.icon = Icons.load(icon);
		this.leftPort = left;
		this.rightPort = right;
		this.leftSource = leftSource;
		this.rightSource = rightSource;
	}

	@Override public String toString() {
		return name;
	}

	public void process() {
		if (isStereo) 
			processFx(leftPort.getFloatBuffer(), rightPort.getFloatBuffer(), gain.getGain());
		else 
			processFx(leftPort.getFloatBuffer());
	}
}
