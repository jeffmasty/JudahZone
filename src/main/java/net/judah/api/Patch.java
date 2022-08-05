package net.judah.api;

import java.io.Serializable;

import lombok.Data;

@Data
public class Patch implements Serializable {
	private static final long serialVersionUID = 1948004569793045131L;
	
	private final String outputPort; 
	private final String inputPort; 

}
