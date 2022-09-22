package net.judah.mixer;

import javax.swing.ImageIcon;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;

/**JudahZone mixer Channels come with built-in compression, reverb and gain */
@Getter
public class Instrument extends LineIn {

    protected final String leftSource;
    protected final String rightSource; // for stereo

    /** Mono channel */
	public Instrument(String name, String sourcePort, JackPort left, ImageIcon icon) {
		super(name, false);
		setIcon(icon);
		this.leftPort = left;
		this.leftSource = sourcePort;
		this.rightSource = null;
	}

	/** Stereo channel */
	public Instrument(String name, String leftSource, String rightSource, JackPort left, JackPort right, ImageIcon icon) {
		super(name, true);
		this.icon = icon;
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
			processFx(leftPort.getFloatBuffer(), rightPort.getFloatBuffer());
		else 
			processFx(leftPort.getFloatBuffer());
	}
}
