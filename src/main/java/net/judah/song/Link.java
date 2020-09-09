package net.judah.song;

import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor @NoArgsConstructor @EqualsAndHashCode(of = "midi") @Data
public class Link {

	private String name;
	private String service;
	private String command;
	private byte[] midi;
	private HashMap<String, Object> props;
	
}
