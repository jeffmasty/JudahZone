package net.judah.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

public class Constants {

	public static final String NL = System.getProperty("line.separator", "\r\n");
	public static final String CUTE_NOTE = "♫ ";
	public static final String FILE_SEPERATOR = System.getProperty("file.separator");
	public static final String TAB = "    ";

	public static final int LEFT_CHANNEL = 0;
	public static final int RIGHT_CHANNEL = 1;
	public static final int STEREO = 2;
	public static final int MONO = 1;

    public static class Gui {
    	public static final int STD_HEIGHT = 18;
    	public static final Insets BTN_MARGIN = new Insets(1,1,1,1);
    	public static final Insets ZERO_MARGIN = new Insets(0,0,0,0);
    	public static final Font BOLD = new Font("Arial", Font.BOLD, 12);
    	public static final Font FONT13 = new Font("Arial", Font.PLAIN, 13);
    	public static final Font FONT12 = new Font("Arial", Font.PLAIN, 12);
    	public static final Font FONT11 = new Font("Arial", Font.PLAIN, 11);
    	public static final Font FONT10 = new Font("Arial", Font.PLAIN, 10);
    	public static final Font FONT9 = new Font("Arial", Font.PLAIN, 9);
    	public static final Border GRAY1 = new LineBorder(Color.GRAY, 1);
    }
	
	
	public static String TEST_PLUGIN_URL = "http://moddevices.com/plugins/tap/reverb";

	public enum Toggles {
		REVERB, CHORUS, PHASER, DISTORTION
	}

    public static int gain2midi(float gain) {
    	return Math.round(gain * 127);
    }

	public static long millisPerBeat(float beatsPerMinute) {
		return Math.round(60000/ beatsPerMinute); //  millis per minute / beats per minute
	}

    public static void infoBox(String infoMessage, String titleBar) {
        JOptionPane.showMessageDialog(null, infoMessage, titleBar, JOptionPane.INFORMATION_MESSAGE);
    }

	@SuppressWarnings("rawtypes")
	public static String prettyPrint(HashMap<String, Object> p) {
		if (p == null) return " null properties";
		StringBuffer b = new StringBuffer();
		for (Entry entry:  p.entrySet())
			b.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
		return b.toString();
	}

	public static void main(String[] args) {
		 UIManager.LookAndFeelInfo[] looks = UIManager.getInstalledLookAndFeels(); 
	        for (UIManager.LookAndFeelInfo look : looks) { 
	            System.out.println(look.getClassName()); 
	        } 
//		for (Info info :  MidiSystem.getMidiDeviceInfo()) {
//			System.out.println(info.getName() + " - " + info.getDescription() + " / " + info.getVendor());
//		}
		try {
			
			System.out.println(MidiSystem.getSequencer().getClass().getCanonicalName());
//			System.out.println("Synthesizer instruments: " + synth.getDeviceInfo().getName() + 
//					" - " + synth.getDeviceInfo().getDescription());
//			System.out.println(Arrays.toString(synth.getDefaultSoundbank().getResources()));
			
			
		} catch (MidiUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static final String PARAM_BPM = "bpm";
	public static final String PARAM_MEAUSRE = "bpb";
	
	
//	// list out standard swing ui setting names
//		public static void main(String[] args) {
//			List<String> colors = new ArrayList<String>();
//			for (Map.Entry<Object, Object> entry : UIManager.getDefaults().entrySet()) {
//			    if (entry.getValue() instanceof Color) {
//			        colors.add((String) entry.getKey()); // all the keys are strings
//			    }
//			}
//			Collections.sort(colors);
//			for (String name : colors)
//			    System.out.println(name);
//		}
		
}

// public static final String IN_PORT = "MIDI in";
// public static final String MPK_IN = "Keyboard";
// public static final String FOOT_IN = "FootPedal";
// public static final String SYNTH_OUT = "midi_out";
// public static final String DR5_OUT = "Drum_out";
// public static final String AUTOMATION_PORT = "Automation";
// public static final String CONTROL = "Control";
