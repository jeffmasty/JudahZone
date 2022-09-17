package net.judah.mixer;

import static net.judah.JudahZone.JUDAHZONE;
import static net.judah.util.Constants.*;

import lombok.Getter;

/**JudahZone mixer Channels come with built-in compression, reverb and gain */
@Getter
public class Instrument extends LineIn {

    protected final String leftSource;
    protected final String leftConnection;
    protected final String rightSource; // for stereo
    protected final String rightConnection;

    /** Mono channel uses left signal */
	public Instrument(String channelName, String sourcePort, String connectionPort) {
		super(channelName, false);
		this.leftSource = sourcePort;
		this.leftConnection = JUDAHZONE + ":" + connectionPort;
		rightSource = rightConnection = null;
	}

	/** Stereo channel */
	public Instrument(String channelName, String[] sourcePorts, String[] connectionPorts) {
		super(channelName, true);
		this.leftSource = sourcePorts[LEFT_CHANNEL];
		this.leftConnection = JUDAHZONE + ":" + connectionPorts[LEFT_CHANNEL];
		this.rightSource = sourcePorts[RIGHT_CHANNEL];
		this.rightConnection = JUDAHZONE + ":" + connectionPorts[RIGHT_CHANNEL];
	}

	@Override
	public String toString() {
		return name + ": " + leftConnection + " . " + rightConnection;
	}

	@Override
	public void process() {
		if (isStereo) 
			processFx(leftPort.getFloatBuffer(), rightPort.getFloatBuffer());
		else 
			processFx(leftPort.getFloatBuffer());
	}
}
