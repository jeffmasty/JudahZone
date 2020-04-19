package net.judah.settings;

import java.util.ArrayList;

import net.judah.JudahZone;
import net.judah.midi.MidiClient;

@SuppressWarnings("serial")
public class Services extends ArrayList<Service> {
	

	public Service byName(String name) {
		for (Service service : this)
			if (name.equals(service.getServiceName()))
					return service;
		return null;
	}

	public Service byClass(Class<?> clazz) {
		for (Service service : this)
			if (service.getClass() == clazz)
				return service;
		return null;
	}


	public MidiClient getMidiClient() {
		return (MidiClient)JudahZone.getServices().byClass(MidiClient.class);
	}

}
