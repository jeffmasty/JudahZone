package net.judah.mixer;

import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.mixer.plugin.Plugin;

@Getter @Log4j
public class Channel {

	private final String name;
	private final boolean isStereo;
	
	@Setter private JackPort leftPort;
	@Setter private JackPort rightPort;
	
	private float gain = 1f;
	@Setter private float gainFactor = 1f;
	
	@Setter private boolean onMute;
	@Setter private boolean muteRecord;
	@Setter private int defaultCC;
	private ChannelGui gui;

	private final String leftSource;
	private final String leftConnection;
	private final String rightSource; // for stereo
	private final String rightConnection;
	
	private final ArrayList<Plugin> plugins = new ArrayList<>();
	
	private transient FloatBuffer buf;
	
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

	public void applyGain() {
		buf = leftPort.getFloatBuffer();
		for (int z = 0; z < buf.capacity(); z++)
			buf.put(buf.get(z) * gain);
		if (!isStereo) return;
		buf = rightPort.getFloatBuffer();
		for (int z = 0; z < buf.capacity(); z++)
			buf.put(buf.get(z) * gain);
	}

	public ChannelGui getGui() {
		if (gui == null)
			gui = new ChannelGui(this);
		return gui;
	}
	
}
