package net.judah.settings;

import java.util.ArrayList;

public class Services extends ArrayList<Service> {
	private static final long serialVersionUID = 9786722563L;

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



}
