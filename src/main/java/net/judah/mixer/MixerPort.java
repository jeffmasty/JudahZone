package net.judah.mixer;

import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class MixerPort {
	public static enum Type {LEFT, RIGHT, MONO};
	
	@Data public static class PortDescriptor {
		private final String name;
		private final Type type;
		private final float defaultGain;
		private final JackPortType portType;
		private final JackPortFlags portFlag;
		
		public PortDescriptor(String name, Type type, JackPortType portType, JackPortFlags portFlag) {
			this.name = name;
			this.type = type;
			this.defaultGain = 1f;
			this.portType = portType;
			this.portFlag = portFlag;
		}
	}

	private final String name;
	private final Type type;

	@Getter @Setter private JackPort port;

	@Getter	@Setter private boolean onLoop = true;
	@Getter @Setter private float gain = 1f;
	@Getter @Setter private float pan = 0f;

//	public MixerPort(String name) {
//
//	}
	
	public MixerPort(PortDescriptor meta, JackPort port) {
		this.name = meta.getName();
		this.type = meta.getType();
		this.gain = meta.getDefaultGain();
		this.port = port;
	}
	
	public MixerPort(MixerPort p) {
		name = p.name;
		type = p.type;
		port = p.port;
		onLoop = p.onLoop;
	}

	public boolean isStereo() {
		return Type.MONO != type;
	}

	public boolean isMono() {
		return Type.MONO == type;
	}

	public String getPortname() {
		if (port == null) return null;
		return port.getName();
	}

	public float getVolume(Type ch) {
		// TODO support stereo channels and adjust Volume to pan setting
		return gain;
	}

	public float getVolume() {
		return gain;
	}
}
