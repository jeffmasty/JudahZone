package net.judah.clock;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackPosition;
import org.jaudiolibs.jnajack.JackTimebaseCallback;
import org.jaudiolibs.jnajack.JackTransportState;

public class JudahTime implements JackTimebaseCallback {

	@Override
	public void updatePosition(JackClient invokingClient, JackTransportState state, int nframes, JackPosition position,
			boolean newPosition) {
		
		
		
	}

}
