package net.judah.settings;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class Command {
	
	public static final String ACTIVE_PARAM = "Active";
	
	/** MIDI transported commands are expected to have the "midi" message as an object member of {@link Command#props} */
	public enum Transport {
		INTERNAL, MIDI
	}

	// @Getter private final Service service;
	@Getter protected final String name;
	@Getter protected final String description;
	@Getter protected final HashMap<String, Class<?>> template;

//	public Command(String name, Service service, HashMap<String, Class<?>> props, String description) {
//		this.name = name;
//		this.description = description;
//		this.service = service;
//		this.props = props;
//	}

	public Command(String name, String description) {
		this(name, description, new HashMap<String, Class<?>>());
	}

	public abstract void execute(HashMap<String, Object> props, int midiData2) throws Exception;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((template == null) ? 0 : template.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Command other = (Command) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (name.equals(other.name))
			return true;
		return false;
	}

	@Override
	public String toString() {
		return name;
		// return "Command [name=" + name + ", service=" + service.getServiceName() + " " + props.values().size() + " properties.";
	}

	@SuppressWarnings("rawtypes")
	public static String toString(HashMap props) {
		String result = " Properties:";
		if (props == null || props.isEmpty()) {
			return result + " none";
		}
		
		for (Object o : props.entrySet()) {
			Map.Entry entry = (Map.Entry)o;
			result += " " + entry.getKey() + ":" + entry.getValue();
		}
		return result;
	}

	
	
}
