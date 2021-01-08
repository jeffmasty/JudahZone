package net.judah.mixer;

import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;

/**JudahZone mixer Channels come with built-in compression, reverb and gain */
public class Channel extends MixerBus {

	@Getter protected final boolean isStereo;
    
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
	
    private FloatBuffer left, right;
	
	/** Mono channel uses left signal */
	public Channel(String channelName, String sourcePort, String connectionPort) {
		super(channelName);
		this.leftSource = sourcePort;
		this.leftConnection = connectionPort;
		rightSource = rightConnection = null;
		isStereo = false;
	}
	
	/** Stereo channel */
	public Channel(String channelName, String[] sourcePorts, String[] connectionPorts) {
		super(channelName);
		this.leftSource = sourcePorts[LEFT_CHANNEL];
		this.leftConnection = connectionPorts[LEFT_CHANNEL];
		this.rightSource = sourcePorts[RIGHT_CHANNEL];
		this.rightConnection = connectionPorts[RIGHT_CHANNEL];
		isStereo = true;
	}
	
	
	@Override
	public int getVolume() {
		return Math.round(gain * 100f / gainFactor);
	}
	
	/** percent of maximum */
	@Override
	public void setVolume(int volume) {
		gain = volume / 100f * gainFactor;
		EffectsGui.volume(this);
		gui.setVolume(volume);
	}

	@Override
	public ChannelGui getGui() {
		if (gui == null) // lazy load
			gui = new ChannelGui(this);
		return gui;
	}

	public void process() {
		left = leftPort.getFloatBuffer();
		if (isStereo)
			right = rightPort.getFloatBuffer();
		
		if (eq.isActive()) {
			eq.process(left, gain);
			if (isStereo())
				eq.process(right, gain);
			
			if (compression.isActive()) {
				left.rewind();
				compression.process(left, 1);
				if (isStereo) {
					right.rewind();
					compression.process(right, 1);
				}
				
				if (reverb.isActive()) { // gain already applied
					left.rewind();
					reverb.process(left, 1);
					if (isStereo) {
						right.rewind();
						reverb.process(right, 1);
					}
				}
			}
			else if (reverb.isActive()) {
				left.rewind();
				reverb.process(left, 1);
				if (isStereo) {
					right.rewind();
					reverb.process(right, 1);
				}
			}
		}
		else if (compression.isActive()) {
			compression.process(left, gain);
			if (isStereo)
				compression.process(right, gain);
			
			if (reverb.isActive()) { // gain already applied
				left.rewind();
				reverb.process(left, 1);
				if (isStereo) {
					right.rewind();
					reverb.process(right, 1);
				}
			}
		}
		
		else if (reverb.isActive()) {
			reverb.process(left, gain);
			if (isStereo)
				reverb.process(right, gain);
		}
		
		else if (gain != 1f) { // standard (no effects) volume
			for (int z = 0; z < left.capacity(); z++)
				left.put(left.get(z) * gain);
			if (!isStereo) return;
			for (int z = 0; z < right.capacity(); z++)
				right.put(right.get(z) * gain);
		}
	}

}
