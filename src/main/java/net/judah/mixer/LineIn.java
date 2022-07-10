package net.judah.mixer;

import static net.judah.JudahZone.JUDAHZONE;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.MainFrame;
import net.judah.plugin.Plugin;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.GuitarTuner;

/**JudahZone mixer Channels come with built-in compression, reverb and gain */
public class LineIn extends Channel {

	@Getter protected final boolean isStereo;

    @Getter @Setter protected JackPort leftPort;
    @Getter @Setter protected JackPort rightPort;

    @Getter protected boolean muteRecord;
    @Getter @Setter protected boolean solo;

    /** set to <code>null</code> for no processing */
    @Getter @Setter protected GuitarTuner tuner;

    @Getter protected final String leftSource;
    @Getter protected final String leftConnection;
    @Getter protected final String rightSource; // for stereo
    @Getter protected final String rightConnection;
    
    @Setter @Getter protected JackPort sync;
    
    @Getter protected final ArrayList<Plugin> plugins = new ArrayList<>();

    /** Mono channel uses left signal */
	public LineIn(String channelName, String sourcePort, String connectionPort) {
		super(channelName);
		this.leftSource = sourcePort;
		this.leftConnection = JUDAHZONE + ":" + connectionPort;
		rightSource = rightConnection = null;
		isStereo = false;
	}

	/** Stereo channel */
	public LineIn(String channelName, String[] sourcePorts, String[] connectionPorts) {
		super(channelName);
		this.leftSource = sourcePorts[LEFT_CHANNEL];
		this.leftConnection = JUDAHZONE + ":" + connectionPorts[LEFT_CHANNEL];
		this.rightSource = sourcePorts[RIGHT_CHANNEL];
		this.rightConnection = JUDAHZONE + ":" + connectionPorts[RIGHT_CHANNEL];
		isStereo = true;
	}

	public void setMuteRecord(boolean muteRecord) {
		this.muteRecord = muteRecord;
		MainFrame.update(this);
	}

	@Override
	public String toString() {
		return name + ": " + leftConnection + " . " + rightConnection;
	}
	
	public void process() {
		FloatBuffer left = leftPort.getFloatBuffer();
		FloatBuffer right = (isStereo) ? rightPort.getFloatBuffer() : null; 
		
		if (this == GuitarTuner.getChannel()) {
			left.rewind();
			MainFrame.update(AudioTools.copy(left));
			left.rewind();
		}
		
		float gain = getVolume() * 0.5f;
		for (int z = 0; z < Constants.bufSize(); z++)
			left.put(left.get(z) * gain);
		if (isStereo)
			for (int z = 0; z < Constants.bufSize(); z++)
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
			cutFilter.process(left);
			if (isStereo())
				cutFilter.process(right);
		}
	}

}
