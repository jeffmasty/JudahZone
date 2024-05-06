package net.judah.midi;

import javax.sound.midi.MidiMessage;

import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;
import net.judah.api.Engine;
import net.judah.util.RTLogger;

/** Wraps a Jack midi port or an internal midi consumer */
@Data
public class MidiPort {
	
	private final JackPort port;
	private final Engine engine;
	
	public MidiPort(JackPort port) {
		if (port == null)
			throw new NullPointerException();
		this.port = port;
		engine = null;	
	}
	
	public MidiPort(Engine engine) {
		if (engine == null)
			throw new NullPointerException();
		this.engine = engine;
		port = null;
	}
	
	public boolean isExternal() {
		return port != null;
	}
	
	public void send(MidiMessage midi, int ticker) {
		if (port == null)
			engine.send(midi, ticker);
		else  
			//JudahMidi.queue(midi, port);
			try { // realtime
				JackMidi.eventWrite(port, ticker, midi.getMessage(), midi.getLength());
			} catch (JackException e) {
				RTLogger.warn(this, e);
			}
	}

	@Override
	public String toString() {
		if (port != null)
			return port.getShortName();
		if (engine != null)
			return engine.getName();
		return "NULL!";
	}
	
}
