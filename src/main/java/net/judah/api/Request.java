package net.judah.api;

import java.util.HashMap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class Request {
	
	@Getter private final String name;
	@Getter private final HashMap<String, Class<?>> template;
	
	public Request(String name) {
		this(name, new HashMap<>());
	}
	
	public abstract void process(HashMap<String, Object> props) throws Exception;
}
