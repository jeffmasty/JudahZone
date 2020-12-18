package net.judah.util;

import java.util.ArrayList;

import net.judah.api.Service;

public class Services extends ArrayList<Service> {

//	@Getter private static Services instance;
	
	public Services() {
//		add(MidiClient.getInstance());
//		add(Mixer.getInstance());
//		add(FluidSynth.getInstance());
	}

//	public Service byName(String name) {
//		for (Service service : this)
//			if (name.equals(service.getServiceName()))
//					return service;
//		return null;
//	}

	public Service byClass(Class<?> clazz) {
		for (Service service : this)
			if (service.getClass() == clazz)
				return service;
		return null;
	}

}
