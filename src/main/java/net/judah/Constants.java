package net.judah;

public class Constants {

	public static final String NL = System.getProperty("line.separator", "\r\n");
	public static final String TAB = "    ";
	public static final int CHANNELS = 16;
	public static final int SAMPLE_RATE = 48000;

	public static final int LEFT_CHANNEL = 0;
	public static final int RIGHT_CHANNEL = 1;
	
	public static String TEST_PLUGIN_URL = "http://moddevices.com/plugins/tap/reverb";

	public enum Toggles {
		REVERB, CHORUS, PHASER, DISTORTION
	}

    public static int gain2midi(float gain) {
    	return Math.round(gain * 127);
    }

}

// public static final String IN_PORT = "MIDI in";
// public static final String MPK_IN = "Keyboard";
// public static final String FOOT_IN = "FootPedal";
// public static final String SYNTH_OUT = "midi_out";
// public static final String DR5_OUT = "Drum_out";
// public static final String AUTOMATION_PORT = "Automation";
// public static final String CONTROL = "Control";
