package net.judah.mixer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class MixerPort {
	public static enum ChannelType {LEFT, RIGHT, MONO};
	
	@Data public static class PortDescriptor {
		private final String name;
		private final ChannelType type;
		
		public PortDescriptor(String name, ChannelType type) {
			this.name = name;
			this.type = type;
		}
	}

	private final String name;
	private final ChannelType type;

	@Getter @Setter private JackPort port;
	@Getter	@Setter private boolean onLoop = true;
	@Getter @Setter private float gain = 1f;
	@Getter @Setter private float pan = 0f;

	public MixerPort(PortDescriptor meta, JackPort port) {
		this.name = meta.getName();
		this.type = meta.getType();
		this.port = port;
	}
	
	public MixerPort(MixerPort p) {
		name = p.name;
		type = p.type;
		port = p.port;
		onLoop = p.onLoop;
		gain = p.gain;
		pan = p.pan;
	}

	public boolean isStereo() {
		return ChannelType.MONO != type;
	}

	public boolean isMono() {
		return ChannelType.MONO == type;
	}

	public String getPortname() {
		if (port == null) return null;
		return port.getName();
	}

	public float getVolume(ChannelType ch) {
		// TODO support stereo channels and adjust Volume to pan setting
		return gain;
	}

	public float getVolume() {
		return gain;
	}
}
