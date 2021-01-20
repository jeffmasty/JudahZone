package net.judah.mixer;

import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.plugin.Plugin;
import net.judah.util.Constants;

/**JudahZone mixer Channels come with built-in compression, reverb and gain */
public class LineIn extends Channel {

	@Getter protected final boolean isStereo;

    @Getter @Setter protected JackPort leftPort;
    @Getter @Setter protected JackPort rightPort;

    @Getter protected boolean muteRecord;
    @Getter @Setter protected boolean solo;

    @Getter protected final String leftSource;
    @Getter protected final String leftConnection;
    @Getter protected final String rightSource; // for stereo
    @Getter protected final String rightConnection;

    @Getter protected final ArrayList<Plugin> plugins = new ArrayList<>();

    private FloatBuffer left, right;

	/** Mono channel uses left signal */
	public LineIn(String channelName, String sourcePort, String connectionPort) {
		super(channelName);
		this.leftSource = sourcePort;
		this.leftConnection = connectionPort;
		rightSource = rightConnection = null;
		isStereo = false;
	}

	/** Stereo channel */
	public LineIn(String channelName, String[] sourcePorts, String[] connectionPorts) {
		super(channelName);
		this.leftSource = sourcePorts[LEFT_CHANNEL];
		this.leftConnection = connectionPorts[LEFT_CHANNEL];
		this.rightSource = sourcePorts[RIGHT_CHANNEL];
		this.rightConnection = connectionPorts[RIGHT_CHANNEL];
		isStereo = true;
	}

	public void setMuteRecord(boolean muteRecord) {
		this.muteRecord = muteRecord;
		if (gui != null) gui.update();
	}

	public void process() {
		left = leftPort.getFloatBuffer();
		if (isStereo)
			right = rightPort.getFloatBuffer();

		float gain = volume / 50f; // per channel gain factor adjustment removed
		for (int z = 0; z < Constants._BUFSIZE; z++)
		    left.put(left.get(z) * gain);
		if (isStereo)
			for (int z = 0; z < Constants._BUFSIZE; z++)
				right.put(right.get(z) * gain);

		if (eq.isActive()) {
			eq.process(left, true);
			if (isStereo)
				eq.process(right, false);
		}
		if (compression.isActive()) {
			compression.process(left, 1);
			if (isStereo)
				compression.process(right, 1);
		}
        if (chorus.isActive()) {
            if (isStereo)
                chorus.processStereo(left, right);
            else
                chorus.processMono(left);
        }
		if (overdrive.isActive()) {
		    overdrive.processAdd(left);
		    if (isStereo)
		        overdrive.processAdd(right);
		}

		if (delay.isActive()) {
			delay.processAdd(left, left, true);
			if (isStereo)
				delay.processAdd(right, right, false);
		}
		if (reverb.isActive() && reverb.isInternal()) {
			reverb.process(left);
			if (isStereo)
				reverb.process(right);
		}
        if (cutFilter.isActive()) {
            cutFilter.process(left, 1);
            if (isStereo())
                cutFilter.process(right, 1);
        }
	}

}
