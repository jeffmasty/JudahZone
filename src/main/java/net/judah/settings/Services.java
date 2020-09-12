package net.judah.settings;

import java.util.ArrayList;

import lombok.Getter;

public class Services extends ArrayList<Service> {

	@Getter private static Services instance;
	
	public Services() {
		assert instance == null;
		instance = this;
	}

	public static Service byName(String name) {
		for (Service service : instance)
			if (name.equals(service.getServiceName()))
					return service;
		return null;
	}

	public static Service byClass(Class<?> clazz) {
		for (Service service : instance)
			if (service.getClass() == clazz)
				return service;
		return null;
	}

}
