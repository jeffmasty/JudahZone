package net.judah.jack;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClientConfig {

	public static final String LEFT_IN = "left_in";
	public static final String RIGHT_IN = "right_in";
	public static final String LEFT_OUT = "left_out";
	public static final String RIGHT_OUT = "right_out";

	@Getter final private String name;
	@Getter final private String[] audioInputNames;
	@Getter final private String[] audioOutputNames;
	@Getter final private String[] midiInputNames;
	@Getter final private String[] midiOutputNames;

	public ClientConfig(String name, String[] audioInputNames, String[] audioOutputNames) {
		this(name, audioInputNames, audioOutputNames, new String[0], new String[0]);
	}

	/** stereo audio client */
	public ClientConfig(String name) {
		this(name, new String[] {LEFT_IN, RIGHT_IN}, new String[] {LEFT_OUT, RIGHT_OUT}, new String[0], new String[0]);
	}
}
