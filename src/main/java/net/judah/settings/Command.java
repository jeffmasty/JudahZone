package net.judah.settings;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

public class Command {
	
	public static final String ACTIVE_PARAM = "Active";
	
	/** MIDI transported commands are expected to have the "midi" message as an object member of {@link Command#props} */
	public enum Transport {
		INTERNAL, MIDI
	}

	@Getter public final String name;
	@Getter public final String description;
	@Getter public final Service service;
	@Getter public final HashMap<String, Class<?>> props;

	public Command(String name, Service service, HashMap<String, Class<?>> props, String description) {
		this.name = name;
		this.description = description;
		this.service = service;
		this.props = props;
	}

	public Command(String name, Service service, String description) {
		this(name, service, new HashMap<String, Class<?>>(), description);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((props == null) ? 0 : props.hashCode());
		result = prime * result + ((service == null) ? 0 : service.hashCode());
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
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (props == null) {
			if (other.props != null)
				return false;
		} else if (!props.equals(other.props))
			return false;
		if (service == null) {
			if (other.service != null)
				return false;
		} else if (!service.equals(other.service))
			return false;
		return true;
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
