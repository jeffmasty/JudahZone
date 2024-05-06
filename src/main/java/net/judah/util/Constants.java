package net.judah.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;

public class Constants {
	private static final ExecutorService threads = Executors.newFixedThreadPool(48);

	// TODO generalize
	public static int sampleRate() { return 48000; }
	public static int bufSize() { return 512; } //TODO 256
	public static float fps() { return sampleRate() / (float)bufSize(); }
	public static final float TUNING = 440;
	/** Digital Interface name */
	@Getter static String di = "UMC1820 MIDI 1"; //di = "Komplete ";
	public static final String LEFT_PORT = "system:playback_1";
	public static final String RIGHT_PORT = "system:playback_2";
	public static final String GUITAR_PORT = "system:capture_5";
	public static final String MIC_PORT = "system:capture_4";
	public static final String CRAVE_PORT = "system:capture_3";
	
	public static final String GUITAR = "Gtr"; 
	public static final String MIC = "Mic";
	public static final String BASS = "Bass";
	public static final String FLUID = "Fluid";
	public static final String MAIN = "Main";
	
    public static final int LEFT = 0;
	public static final int RIGHT = 1;
	public static final int STEREO = 2;
	public static final int MONO = 1;

	public static final String NL = System.getProperty("line.separator", "\r\n");
	public static final String CUTE_NOTE = "♫ ";
	public static final String DOT_MIDI = ".mid";

	/** milliseconds between checking the update queue */
	public static final int GUI_REFRESH = 8;
	public static final long DOUBLE_CLICK = 500;
	public static final float TO_100 = 0.7874f; // 127 <--> 100

	@Getter static float[] reverseLog = new float[100];
	static {
		for (int i = 0; i < reverseLog.length; i++)
			reverseLog[i] = logarithmic(i, 0, 1);
	}
	
    /**@param data2 0 to 127
     * @return data2 / 127 */
	public static float midiToFloat(int data2) {
		return data2 * 0.00787f;
	}

    public static float computeTempo(long millis, int beats) {
    	return bpmPerBeat(millis / (float)beats);
    }
    
    public static float bpmPerBeat(float msec) {
        return 60000 / msec;
    }

	public static long millisPerBeat(float beatsPerMinute) {
		return Math.round(60000/ beatsPerMinute); //  millis per minute / beats per minute
	}
	
	public static float toBPM(long delta, int beats) {
		return 60000 / (delta / (float)beats);
	}

	public static float midiToHz(int data1) {
        return (float)(Math.pow(2, (data1 - 57d) / 12d)) * TUNING;
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
	
	public static int rotary(int input, int size, boolean up) {
		 input += (up ? 1 : -1);
		 if (input >= size) 
    		input = 0;
    	if (input < 0) 
    		input = size - 1;
    	return input;
	}

    /** see https://stackoverflow.com/a/846249 */ 	
	public static float logarithmic(int percent, float min, float max) {
		
		// percent will be between 0 and 100
		final int minp = 1;
		final int maxp = 100;
		assert percent <= max && percent >= min;
		
		// The result should be between min and max
		if (min <= 0) min = 0.0001f;
		double minv = Math.log(min);
		double maxv = Math.log(max);
	
		// calculate adjustment factor
		double scale = (maxv-minv) / (maxp-minp);
		return (float)Math.exp(minv + scale * (percent - minp));
	}
	
	public static void sleep(long millis) {
	    try {
	        Thread.sleep(millis);
	    } catch(Throwable t) {System.err.println(t.getMessage());}
	}
	
	public static void timer(long msec, final Runnable r) {
	    threads.execute(()->{
    		sleep(msec);
    		r.run();
	    });
	}

	public static void execute(Runnable r) {
		threads.execute(r);
	}
	
}
