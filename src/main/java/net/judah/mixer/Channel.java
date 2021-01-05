package net.judah.mixer;

import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;

/**JudahZone mixer Channels come with built-in compression, reverb and gain */
public class Channel {

	@Getter protected final String name;
	@Getter protected final boolean isStereo;
    
    @Getter private Compression compression = new Compression();
    @Getter private Reverb reverb = new Reverb();
    
    @Getter @Setter protected JackPort leftPort;
    @Getter @Setter protected JackPort rightPort;
	
    @Getter protected float gain = 1f;
    @Getter @Setter protected float gainFactor = 1f;
	
    @Getter @Setter protected boolean onMute;
    @Getter @Setter protected boolean muteRecord;
    @Getter @Setter protected boolean solo;
    @Getter @Setter protected int defaultCC;
    protected ChannelGui gui;

    @Getter protected final String leftSource;
    @Getter protected final String leftConnection;
    @Getter protected final String rightSource; // for stereo
    @Getter protected final String rightConnection;
	
    @Getter protected final ArrayList<Plugin> plugins = new ArrayList<>();
	
    protected transient FloatBuffer buf;
	
	/** Mono channel uses left signal */
	public Channel(String channelName, String sourcePort, String connectionPort) {
		this.name = channelName;
		this.leftSource = sourcePort;
		this.leftConnection = connectionPort;
		rightSource = rightConnection = null;
		isStereo = false;
	}
	
	/** Stereo channel */
	public Channel(String channelName, String[] sourcePorts, String[] connectionPorts) {
		this.name = channelName;
		this.leftSource = sourcePorts[LEFT_CHANNEL];
		this.leftConnection = connectionPorts[LEFT_CHANNEL];
		this.rightSource = sourcePorts[RIGHT_CHANNEL];
		this.rightConnection = connectionPorts[RIGHT_CHANNEL];
		isStereo = true;
	}
	
	/** percent of maximum */
	public void setVolume(int volume) {
		gain = volume / 100f * gainFactor;
		if (gui != null) {
			gui.setVolume(volume);
		}
	}

	public ChannelGui getGui() {
		if (gui == null) // lazy load
			gui = new ChannelGui(this);
		return gui;
	}
    

	public void process() {
		
		if (reverb.isActive()) {
			reverb.process(leftPort.getFloatBuffer(), gain);
			if (isStereo)
				reverb.process(rightPort.getFloatBuffer(), gain);

			if (compression.isActive()) { // gain already applied
				leftPort.getFloatBuffer().rewind();
				compression.process(leftPort.getFloatBuffer(), 1f); 
				if (isStereo) {
					rightPort.getFloatBuffer().rewind();
					compression.process(rightPort.getFloatBuffer(), 1f);
				}
			}
		}
		
		else if (compression.isActive()) {
			compression.process(leftPort.getFloatBuffer(), gain);
			if (isStereo)
				compression.process(rightPort.getFloatBuffer(), gain);
		}

		else if (gain != 1f) { // standard (no effects) volume
			buf = leftPort.getFloatBuffer();
			for (int z = 0; z < buf.capacity(); z++)
				buf.put(buf.get(z) * gain);
			if (!isStereo) return;
			buf = rightPort.getFloatBuffer();
			for (int z = 0; z < buf.capacity(); z++)
				buf.put(buf.get(z) * gain);
		}
	}

}
