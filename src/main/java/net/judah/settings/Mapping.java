package net.judah.settings;

import java.io.Serializable;
import java.util.Properties;

import lombok.Getter;
import net.judah.midi.Midi;

public class Mapping implements Serializable {
	private static final long serialVersionUID = 391031963502031826L;

	@Getter final String serviceName;
	@Getter final String commandName;
	final byte[] midiData;
	@Getter final Properties props;
	@Getter final boolean isDynamic;

	transient Midi midi; // not serializing correctly

	public Mapping(Command command, Midi midi, Properties props) {
		this(command, midi, props, false);
	}
	public Mapping(Command command, Midi midi, Properties props, boolean isDynamic) {
		this.serviceName = command.service.getServiceName();
		this.commandName = command.name;
		this.midiData = midi.getMessage();
		this.midi = midi;
		this.props = props;
		this.isDynamic = isDynamic;
	}

	public Midi getMidi() {
		if (midi == null) {
			midi = new Midi(midiData);
		}
		return midi;
	}

	@Override
	public String toString() {
		return "Mapping " + serviceName + "." + commandName + " for " + new Midi(midiData);
	}

}
