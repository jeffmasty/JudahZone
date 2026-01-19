package net.judah.bridge;

import judahzone.api.Ports;
import judahzone.javax.JavaxHelper;
import judahzone.jnajack.JackHelper;
import lombok.Getter;
import net.judah.JudahZone;

public class AudioEngine {

	/** Audio Engine */
	public enum Type { JACK, JAVAX }
	/** Audio Engine */
	@Getter public static final Type type = Type.JACK;

	private static Ports.Provider ports;

	public static Ports.Provider getPorts() {
		if (ports == null)
			ports = type == Type.JACK ?
				new JackHelper(JudahZone.getInstance().getMidi(), JudahZone.getInstance())
				: new JavaxHelper();
		return ports;
	}



}
