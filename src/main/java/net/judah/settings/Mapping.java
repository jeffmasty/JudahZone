package net.judah.settings;

import java.io.Serializable;
import java.util.Properties;

import javax.sound.midi.ShortMessage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.judah.midi.Midi;

@AllArgsConstructor
public class Mapping implements Serializable {
	private static final long serialVersionUID = 391031963502031826L;

	@Getter final String serviceName;
	@Getter final String commandName;
	@Getter final byte[] midiData;
	@Getter final Properties props;
	@Getter final boolean isDynamic;

	transient ShortMessage midi; // not serializing correctly

	public Mapping(Command command, ShortMessage midi, Properties props) {
		this(command, midi, props, false);
	}
	public Mapping(Command command, ShortMessage midi, Properties props, boolean isDynamic) {
		this.serviceName = command.service.getServiceName();
		this.commandName = command.name;
		this.midiData = midi.getMessage();
		this.midi = midi;
		this.props = props;
		this.isDynamic = isDynamic;
	}

	public ShortMessage getMidi() {
		if (midi == null) {
			midi = new Midi(midiData);
		}
		return midi;
	}

	@Override
	public String toString() {
		return "Mapping " + serviceName + "." + commandName + " for " + new Midi(midiData) + (isDynamic ? " dynamic " : " ") + 
				Command.toString(props);
	}

}
