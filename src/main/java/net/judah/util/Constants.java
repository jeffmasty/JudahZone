package net.judah.util;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import lombok.Getter;
import net.judah.gui.Pastels;
import net.judah.midi.Midi;

public class Constants {
	private static ClassLoader loader = Constants.class.getClassLoader();

	// TODO generalize
	private static int _SAMPLERATE = 48000;
	private static int _BUFSIZE = 512;//TODO 256
	public static int sampleRate() { return _SAMPLERATE; }
	public static int bufSize() { return _BUFSIZE; }
	public static final float TUNING = 440;
	/** Digital Interface name */
	@Getter static String di = "UMC1820 MIDI 1"; //return "Komplete ";
	
	public static final Midi DUMBDRUM = create(1, 0);
	public static final Midi BASSDRUM = create(36, 100);
	static Midi create(int dat1, int velocity) {
		Midi result = null;
		try { result = new Midi(Midi.NOTE_ON, 9, dat1, velocity);
		} catch (InvalidMidiDataException e) {e.printStackTrace();}
		return result;
	}

    public static JComponent resize(JComponent c, Dimension d) {
		c.setMaximumSize(d);
		c.setPreferredSize(d);
		return c;
    }

    public static final int LEFT_CHANNEL = 0;
	public static final int RIGHT_CHANNEL = 1;
	public static final int STEREO = 2;
	public static final int MONO = 1;

	public static final String GUITAR = "Gtr"; 
	public static final String MIC = "Mic";
	public static final String CRAVE = "Bass";
	public static final String CRAVE_PORT = "system:capture_3";
	public static final String FLUID = "Fluid";
	public static final String MAIN = "Main";
		
	public static final String NL = System.getProperty("line.separator", "\r\n");
	public static final String CUTE_NOTE = "♫ ";
	public static final String FILE_SEPERATOR = System.getProperty("file.separator");
	public static final String TAB = "    ";
	public static final String FX = "Fx";
	public static final String NONE = "none";
	public static final String DOT_MIDI = ".mid";

	/** milliseconds between checking the update queue */
	public static final int GUI_REFRESH = 9;
	public static final long DOUBLE_CLICK = 400;

    public static interface Gui {

    	int STD_HEIGHT = 18;
    	Insets BTN_MARGIN = new Insets(1,1,1,1);
    	Insets ZERO_MARGIN = new Insets(0,0,0,0);
    	
    	Border HIGHLIGHT = BorderFactory.createLineBorder(Color.BLACK, 1);
    	Border NONE = BorderFactory.createLineBorder(Pastels.MY_GRAY, 1);
    	Border NO_BORDERS = new EmptyBorder(BTN_MARGIN);

    	Font BOLD = new Font("Arial", Font.BOLD, 11);
    	Font BOLD13 = new Font("Arial", Font.BOLD, 13);
    	Font FONT13 = new Font("Arial", Font.PLAIN, 13);
    	Font FONT12 = new Font("Arial", Font.PLAIN, 12);
    	Font FONT11 = new Font("Arial", Font.PLAIN, 11);
    	Font FONT10 = new Font("Arial", Font.PLAIN, 10);
    	Border GRAY1 = new LineBorder(Color.GRAY, 1);
    	Font FONT9 = new Font("Arial", Font.PLAIN, 9);
    }
    
    public static JPanel duo(Component left, Component right) {
		JPanel result = new JPanel();
		result.setLayout(new GridLayout(1, 2));
		result.add(left);
		result.add(right);
		return result;
	}

    public static JPanel wrap(Component... items) {
		JPanel result = new JPanel();
		for (Component p : items)
			result.add(p);
		return result;
	}
	
    public static int gain2midi(float gain) {
    	return Math.round(gain * 127);
    }
    public static float midi2float(int data2) {
    	float result = data2/127f;
    	assert result <= 1f : data2 + " vs. " + (data2/127f);
    	return result;
    }

    public static float computeTempo(long millis, int beats) {
    	return bpmPerBeat(millis / (float)beats);
    }
    
    public static float bpmPerBeat(float msec) {
        return 60000f / msec;
    }

	public static long millisPerBeat(float beatsPerMinute) {
		return Math.round(60000/ beatsPerMinute); //  millis per minute / beats per minute
	}
	
	public static float toBPM(long delta, int beats) {
		return 60000 / (delta / beats);
	}

    public static void infoBox(String infoMessage, String titleBar) {
        JOptionPane.showMessageDialog(null, infoMessage, titleBar, JOptionPane.INFORMATION_MESSAGE);
    }
    public static String inputBox(String infoMessage) {
        return JOptionPane.showInputDialog(infoMessage);
    }
    
	@SuppressWarnings("rawtypes")
	public static String prettyPrint(HashMap<String, Object> p) {
		if (p == null) return " null properties";
		StringBuffer b = new StringBuffer();
		for (Entry entry:  p.entrySet())
			b.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
		return b.toString();
	}

	public static HashMap<String, Class<?>> template(String key, Class<?> clazz) {
		HashMap<String, Class<?>> result = new HashMap<>();
		result.put(key, clazz);
		return result;
	}

	public static float midiToHz(int data1) {
        return (float)(Math.pow(2, (data1 - 57d) / 12d)) * TUNING;
    }

	public static Midi transpose(Midi in, int steps) throws InvalidMidiDataException {
		if (steps == 0) return in;
		return new Midi(in.getCommand(), in.getChannel(), in.getData1() + steps, in.getData2());
	}

	public static Midi transpose(Midi in, int steps, float gain) throws InvalidMidiDataException {
		if (steps == 0 && gain >= 1f) return in;
		return new Midi(in.getCommand(), in.getChannel(), in.getData1() + steps, (int)(in.getData2() * gain));
	}

	public static Midi gain(Midi in, float gain) throws InvalidMidiDataException {
		if (gain == 1f) return in;
		return new Midi(in.getCommand(), in.getChannel(), in.getData1(), (int)(in.getData2() * gain));
	}

	public static Midi transpose(Midi in, int steps, int channel) throws InvalidMidiDataException {
		return new Midi(in.getCommand(), channel, in.getData1() + steps, in.getData2());
	}

	public static void timer(long msec, Runnable r) {
	    new Thread( () -> {
	        try {
	            Thread.sleep(msec);
	            r.run();
	        } catch(Throwable t) {
	            System.err.println(t.getMessage());
	        }
	    }).start();
	}

	public static void sleep(long millis) {
	    try {
	        Thread.sleep(millis);
	    } catch(Throwable t) {
	        System.err.println(t.getMessage());
	    }
	}

	public static File resource(String filename) {
	    try {
	        return new File(loader.getResource(filename).getFile());
	    } catch (Throwable t) {
	        Console.warn(t);
	        return null;
	    }
	}


	public static void main2(String[] args) {
		System.out.println(toBPM(1000, 1) + " bpm");
		System.out.println(toBPM(3000, 3) + " bpm");
		System.out.println(toBPM(1000, 2) + " bpm");
		//for (Info info :  MidiSystem.getMidiDeviceInfo()) {
		//	System.out.println(info.getName() + " - " + info.getDescription() + " / " + info.getVendor()); }
		try {
			System.out.println(MidiSystem.getSequencer().getClass().getCanonicalName());
			//System.out.println("Synthesizer instruments: " + synth.getDeviceInfo().getName() +
			//		" - " + synth.getDeviceInfo().getDescription());
			//System.out.println(Arrays.toString(synth.getDefaultSoundbank().getResources()));
		} catch (MidiUnavailableException e) { e.printStackTrace(); }
	}

    public static void writeToFile(File file, String content) {
        new Thread(() -> {
            try { Files.write(Paths.get(file.toURI()), content.getBytes());
            } catch(IOException e) {RTLogger.warn("Constants.writeToFile", e);}
        }).start();
    }
    
	public static Object ratio(int data2, List<?> input) {
        return input.get((int) ((data2 - 1) / (100 / (float)input.size())));
	}
	public static Object ratio(int data2, Object[] input) {
		return input[(int) ((data2 - 1) / (100 / (float)input.length))];
	}
	public static int ratio(long data2, long size) {
		return (int) (data2 / (100 / (float)size));
	}

	public static boolean isNumeric(String str) {
        return str != null && str.matches("[-+]?\\d*\\.?\\d+");
    }
	
	/**@param data2 0 to 127
	 * @return data2 / 127 */
	public static float reverseVelocity(int data2) {
		return data2 * 0.007874f;
	}

	/**@param supplied buffer
	 * @param pos
	 * @param numBytes
	 * @return little endiean data to long from pos in the supplied buffer */
	public static long getLE(byte[] buffer, int pos, int numBytes) {
		numBytes --;
		pos += numBytes;

		long val = buffer[pos] & 0xFF;
		for (int b=0 ; b<numBytes ; b++) 
			val = (val << 8) + (buffer[--pos] & 0xFF);

		return val;
	}

	/**put val as little endian at pos in supplied buffer
	 * @param val
	 * @param buffer
	 * @param pos
	 * @param numBytes */
	public static void putLE(long val, byte[] buffer, int pos, int numBytes) {
		for (int b=0 ; b<numBytes ; b++) {
			buffer[pos] = (byte) (val & 0xFF);
			val >>= 8;
			pos ++;
		}
	}

    /** see https://stackoverflow.com/a/846249 */ 	
	public static float logarithmic(int percent, float min, float max) {
		
		// percent will be between 0 and 100
		final int minp = 1;
		final int maxp = 100;
		assert percent <= max && percent >= min;
		
		// The result should be between min and max
		if (min <= 0) min = 0.001f;
		double minv = Math.log(min);
		double maxv = Math.log(max);
	
		// calculate adjustment factor
		double scale = (maxv-minv) / (maxp-minp);
		return (float)Math.exp(minv + scale * (percent - minp));
	}
	
	@Getter static float[] reverseLog = new float[100];
	static {
		for (int i = 0; i < reverseLog.length; i++)
			reverseLog[i] = logarithmic(i, 0, 1);
	}
	
	
}
