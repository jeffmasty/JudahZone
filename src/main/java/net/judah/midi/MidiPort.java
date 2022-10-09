package net.judah.midi;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;
import net.judah.api.Engine;

/** Wraps a Jack midi port or an internal midi consumer */
@Data
public class MidiPort {
	
	private final JackPort port;
	private final Engine receiver;
	
	public MidiPort(JackPort port) {
		if (port == null)
			throw new NullPointerException();
		this.port = port;
		receiver = null;	
	}
	
	public MidiPort(Engine engine) {
		if (engine == null)
			throw new NullPointerException();
		this.receiver = engine;
		port = null;
	}
	
	public boolean isExternal() {
		return port != null;
	}
	
	public void send(ShortMessage midi, int ticker) {
		if (port == null)
			receiver.send(midi, ticker);
		else
			JudahMidi.queue(midi, port);
	}

	@Override
	public String toString() {
		if (port != null)
			return port.getShortName();
		if (receiver != null)
			return receiver.getName();
		return "NULL!";
	}
	
}
