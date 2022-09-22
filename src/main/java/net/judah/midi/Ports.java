package net.judah.midi;

import java.util.ArrayList;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.api.Engine;

public class Ports extends ArrayList<MidiPort> {

	public MidiPort get(String name) {
		for (MidiPort p : this) {
			if (p.isExternal()) {
				if (p.getPort().getShortName().equals(name))
					return p;
			}
			else if (p.getReceiver() == null)
				throw new NullPointerException(p.toString());
			else if (p.getReceiver().getName().equals(name))
					return p;
		}
		throw new NullPointerException(name);
	}
	
	public MidiPort get(JackPort port) {
		for (MidiPort p : this)
			if (p.getPort() == port)
				return p;
		return null;
	}
	
	public MidiPort get(Engine engine) {
		for (MidiPort p: this)
			if (p.getReceiver() == engine)
				return p;
		return null;
	}
}
