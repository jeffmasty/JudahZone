package net.judah.util;

import java.awt.*;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Midi;
import net.judah.mixer.Channel;

public class Constants {
	
	// TODO generalize
	private static int _SAMPLERATE = 48000;
	private static int _BUFSIZE = 512;//TODO:(256)
	public static int sampleRate() { return _SAMPLERATE; }
	public static int bufSize() { return _BUFSIZE; }
	/** Digital interface name */
	@Getter static String di = "UMC1820 MIDI 1"; //return "Komplete ";
	
	// TODO
    public static final File ROOT = new File("/home/judah/git/JudahZone/resources/");
    public static final File defaultFolder = new File(ROOT, "Songs/");
    public static final File defaultSetlist = new File(defaultFolder, "list1.songs");
    public static final File defaultDrumFile = new File(ROOT, "patterns/Drum1");
	public static final File SHEETMUSIC = new File(System.getProperty("user.home"), "sheets");
	public static final File SAMPLES = new File(ROOT, "samples");

	private static final String MAIN = "Mains";
	private static final String DRUMS = "Drumtrack";
	private static final String LOOPA = "Loop A";
	private static final String LOOPB = "Loop B";
	private static final String[] DEFAULT_OUT = new String[] {MAIN, DRUMS, LOOPA, LOOPB};

    public static class Param {
		public static final String ACTIVE = "active";
		/** channel to send trick track midi out on */
		public static final String CHANNEL = "channel";
		public static final String BPM = "bpm";
		public static final String TEMP = BPM;
		public static final String MEASURE = "bpb";
		public static final String BPB = MEASURE;
		public static final String FILE = "file";
		public static final String NAME = "name";
		public static final String INDEX = "index";
		public static final String VALUE = "value";
		public static final String TYPE = "type";
		public static final String GAIN = "volume";
		public static final String LOOP = "loop";
		public static final String SEQUENCE = "sequence";
		public static final String MAX = "max";
		public static final String STEPS = "steps";
		public static final String IMAGE = "image";
		public static final String FLUID = "fluid";


		public static boolean parseActive(HashMap<String, Object> props) {
			return (Boolean.parseBoolean(props.get(ACTIVE).toString()));
		}

		public static String parseString(String key, HashMap<String, Object> props) {
			return props.get(key).toString();
		}

		public static HashMap<String, Class<?>> singleTemplate(String name, Class<?> clazz) {
			HashMap<String, Class<?>> params = new HashMap<>();
			params.put(name, clazz);
			return params;
		}

		public static HashMap<String, Class<?>> activeTemplate() {
			return singleTemplate(ACTIVE, Boolean.class);
		}

	}

    public static final Dimension MAX = new Dimension(122, 30);
    public static JComponent max(JComponent c) {
		c.setMaximumSize(MAX);
		c.setPreferredSize(MAX);
		return c;
    }
    
	public static final String NL = System.getProperty("line.separator", "\r\n");
	public static final String CUTE_NOTE = "♫ ";
	public static final String FILE_SEPERATOR = System.getProperty("file.separator");
	public static final String TAB = "    ";

	public static final int LEFT_CHANNEL = 0;
	public static final int RIGHT_CHANNEL = 1;
	public static final int STEREO = 2;
	public static final int MONO = 1;

	/** milliseconds between checking the update queue */
	public static final int GUI_REFRESH = 5;
	public static final long DOUBLE_CLICK = 400;

    public static interface Gui {
    	
    	Border HIGHLIGHT = BorderFactory.createLineBorder(Pastels.GREEN, 4);
    	Border NONE = BorderFactory.createLineBorder(Pastels.EGGSHELL, 4);

    	int STD_HEIGHT = 18;
    	
    	Insets BTN_MARGIN = new Insets(1,1,1,1);
    	Insets ZERO_MARGIN = new Insets(0,0,0,0);
    	Font BOLD = new Font("Arial", Font.BOLD, 11);
    	Font BOLD13 = new Font("Arial", Font.BOLD, 13);
    	Font FONT13 = new Font("Arial", Font.PLAIN, 13);
    	Font FONT12 = new Font("Arial", Font.PLAIN, 12);
    	Font FONT11 = new Font("Arial", Font.PLAIN, 11);
    	Font FONT10 = new Font("Arial", Font.PLAIN, 10);
    	Border GRAY1 = new LineBorder(Color.GRAY, 1);
    	Dimension SLIDER_SZ = new Dimension(86, 40);
    	Font FONT9 = new Font("Arial", Font.PLAIN, 9);

    	Border NO_BORDERS = new EmptyBorder(BTN_MARGIN);
    	Border FOCUS_BORDER = new LineBorder(Color.BLACK, 2, true);

    }

    public static void attachKeyListener(Container p, KeyListener l) {
        for(Component c : p.getComponents()) {
            c.addKeyListener(l);
            if (c instanceof Container)
                attachKeyListener((Container)c, l);
        }
    }

    
    public static final Midi DUMBDRUM = create(1, 0);
	public static final Midi BASSDRUM = create(36, 100);
	static Midi create(int dat1, int velocity) {
		Midi result = null;
		try { result = new Midi(Midi.NOTE_ON, 9, dat1, velocity);
		} catch (InvalidMidiDataException e) {e.printStackTrace();}
		return result;
	}
	
	/**Normalize midi track[0] 
	 * @return the length in resolved bars of a full sequence 
	 * (some midi files do not define ticks to the end of the bar) */
	public static int requeueBeats(Sequence sequence) {
		int result = (int) Math.ceil( (sequence.getTickLength() - 1) / (float)sequence.getResolution());
		// long old = sequence.getTickLength();
		long normalized = sequence.getResolution() * result;
		sequence.getTracks()[0].add(new MidiEvent(DUMBDRUM, normalized));
		// RTLogger.log(sequence, "From " + old + " to " + normalized + " @ " + sequence.getResolution());
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

	public static float toBPM(long delta, int beats) {
		return 60000 / (delta / beats);
	}

	public static HashMap<String, Class<?>> template(String key, Class<?> clazz) {
		HashMap<String, Class<?>> result = new HashMap<>();
		result.put(key, clazz);
		return result;
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

	private static ClassLoader loader = Constants.class.getClassLoader();
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


	public static JMenu createMixerMenu(String lbl) {
	    JMenu result = new JMenu(lbl);
	    for (String out : DEFAULT_OUT)
	        result.add(new JMenuItem(out));
	    for (Channel ch : JudahZone.getChannels())
	        result.add(new JMenuItem(ch.getName()));
        return result;
	}

	public static JComboBox<String> createMixerCombo() {
	    ArrayList<String> channels = new ArrayList<>(Arrays.asList(DEFAULT_OUT));
        for (Channel c : JudahZone.getChannels())
            channels.add(c.getName());
        JComboBox<String> result = new JComboBox<>(
                channels.toArray(new String[channels.size()]));
	    return result;
	}

    public static Channel getChannel(String name) {
        switch(name) {
            case MAIN: JudahZone.getMains();
            case DRUMS: return JudahZone.getLooper().getDrumTrack();
            case LOOPA: return JudahZone.getLooper().getLoopA();
            case LOOPB: return JudahZone.getLooper().getLoopB();
        }
        return JudahZone.getChannels().byName(name);
    }

    public static Channel getChannel(int idx) {
        switch(idx) {
            case 0: return JudahZone.getMains();
            case 1: return JudahZone.getLooper().getDrumTrack();
            case 2: return JudahZone.getLooper().getLoopA();
            case 3: return JudahZone.getLooper().getLoopB();
        }
        return JudahZone.getChannels().get(idx - 4);
    }

    public static void writeToFile(File file, String content) {
        new Thread(() -> {
            try { Files.write(Paths.get(file.toURI()), content.getBytes());
            } catch(IOException e) {RTLogger.warn("Constants.writeToFile", e);}
        }).start();
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
	
	/** untested */
	public static int reverseLog(float val, float min, float max) {
		// input should be between min and max
		assert val >= min && val <= max;

		// result will be between 0 and 100
		var minp = 0;
		var maxp = 100;
		
		var minv = Math.log(min);
		var maxv = Math.log(max);
		// calculate adjustment factor
		var scale = (maxv-minv) / (maxp-minp);
		
		return Math.round((float)((Math.log(val)-minv) / scale + minp));
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
	
}
