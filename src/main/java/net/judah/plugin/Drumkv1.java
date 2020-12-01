package net.judah.plugin;

import static org.jaudiolibs.jnajack.JackPortFlags.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPortType;

import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j;

@Log4j
public class Drumkv1 {
	
	@Data
	public static class Drumkv1Params {
		final File drumkit;
		final String portname;
	}
	
	public static final String FILE_PARAM = "drumkv1";
	public static final String PORT_PARAM = "midiport";
	public static final String shellCommand = "drumkv1_jack -g "; // -g = no gui
	
	public static final File defaultDrumkit = new File("/home/judah/git/JudahZone/resources/drumkv1/GMRockKit/GMRockKit.drumkv1");
	public static final String MIDI_IN = "drumkv1:in";
	public static final String OUT_L = "drumkv1:out_1";
	public static final String OUT_R = "drumkv1:out_2";
	
	@Getter private static Drumkv1 instance;
	@Getter private Process process;
	@Getter private File drumkit;
	
	private String cmd;
	
	public Drumkv1(File drumkit) throws IOException, JackException { 
		this(drumkit, true); }
	
	/**Opens and connects a solitary Drumkv1 application
	 * @param drumkit file
	 * @param portname  JudahZone midi port to connect to drumkv1 */
	@SuppressWarnings("deprecation")
	public Drumkv1(File drumkit, boolean makeConnections) throws IOException, JackException {
		
		if (instance != null) {
			log.warn("Closing previous instance!!");
			instance.close();
			try { Thread.sleep(25); } catch (Throwable t) { }
		}
		instance = this;
		
		this.drumkit = drumkit;
		cmd = shellCommand + drumkit.getAbsolutePath();
		process = Runtime.getRuntime().exec(cmd);
		// make sure to close the process on exit..
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override public void run() { close(); }});
		
	    Jack jack = Jack.getInstance();
	    try { // wait a bit for drumkv1 to create its ports
	    	while (jack.getPorts(OUT_R, null, null).length == 0 || 
	    			jack.getPorts(MIDI_IN, null, null).length == 0) {
		    	Thread.sleep(50); }
	    } catch (InterruptedException e) { }

 		log.debug("drumkv1 process created");
 		if (makeConnections) makeConnections();
	}

	public Drumkv1() throws IOException, JackException {
		this(defaultDrumkit, false);
	}
	
	@SuppressWarnings("deprecation")
	public void makeConnections() throws IOException, JackException{
		Jack jack = Jack.getInstance();
		String[] out1 = jack.getPorts("out_1", JackPortType.AUDIO, EnumSet.of(JackPortIsOutput));
		String[] out2 = jack.getPorts("out_2", JackPortType.AUDIO, EnumSet.of(JackPortIsOutput));
		if (out1.length < 1 || out2.length < 1) 
			log.warn("Danger Will Robinson. " + Arrays.toString(out1) + " and " + Arrays.toString(out2));
		
		for (String s : out1)
			if (s.contains("drumkv1"))
				try { jack.connect(s, "system:playback_1");
				} catch (Exception e) { log.warn(e.getMessage(), e); }
		for (String s : out2)
			if (s.contains("drumkv1"))
				try { jack.connect(s, "system:playback_2");
				} catch (Exception e) { log.warn(e.getMessage(), e); }

	}

	public void close() {
		if (process != null)
			process.destroy();	
		process = null;
		instance = null;
	}

}
