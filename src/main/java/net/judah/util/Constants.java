package net.judah.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.midi.InvalidMidiDataException;

import lombok.Getter;
import net.judah.midi.Midi;

public class Constants {
	private static final ClassLoader loader = Constants.class.getClassLoader();
	private static final ExecutorService threads = Executors.newFixedThreadPool(32);

	// TODO generalize
	public static int sampleRate() { return 48000; }
	public static int bufSize() { return 512; } //TODO 256
	public static final float TUNING = 440;
	/** Digital Interface name */
	@Getter static String di = "UMC1820 MIDI 1"; //return "Komplete ";
	public static final float TO_100 = 0.7874f; // 127 <--> 100
	
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
	public static final String NONE = "none";
	public static final String DOT_MIDI = ".mid";

	/** milliseconds between checking the update queue */
	public static final int GUI_REFRESH = 7;
	public static final long DOUBLE_CLICK = 400;

    /**@param data2 0 to 127
     * @return data2 / 127 */
	public static float midiToFloat(int data2) {
		return data2 * 0.00787f;
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

	public static File resource(String filename) {
	    try {
	        return new File(loader.getResource(filename).getFile());
	    } catch (Throwable t) {
	        Console.warn(t);
	        return null;
	    }
	}

    public static void writeToFile(File file, String content) {
    	execute(() -> {
            try { Files.write(Paths.get(file.toURI()), content.getBytes());
            } catch(IOException e) {RTLogger.warn("Constants.writeToFile", e);}
        });
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

	public static void sleep(long millis) {
	    try {
	        Thread.sleep(millis);
	    } catch(Throwable t) {System.err.println(t.getMessage());}
	}
	
	public static void timer(long msec, final Runnable r) {
	    threads.execute(()->{
	    	new Thread(()->{
	    		sleep(msec);
	    		r.run();}
	    	).start();
	    });
	    
	}

	public static void execute(Runnable r) {
		threads.execute(r);
	}
	
	
}
