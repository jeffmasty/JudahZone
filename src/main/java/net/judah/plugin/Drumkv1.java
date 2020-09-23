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
import net.judah.midi.JudahPort;

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
	public static final File defaultDrumkit = new File("/home/judah/Tracks/drumkv1/TR808EmulationKit/TR808EmulationKit.drumkv1");
	public static final String MIDI_IN = "drumkv1:in";
	public static final String OUT_L = "drumkv1:out_1";
	public static final String OUT_R = "drumkv1:out_2";
	
	@Getter private static Drumkv1 instance;
	
	@Getter private Process process;
	@Getter private String portname;
	@Getter private File drumkit;
	
	private String cmd;
	
	/**Opens and connects a solitary Drumkv1 application
	 * @param drumkit file
	 * @param portname  JudahZone midi port to connect to drumkv1 */
	@SuppressWarnings("deprecation")
	public Drumkv1(File drumkit, String portname, boolean makeConnections) throws IOException, JackException {
		
		if (instance != null) {
			log.warn("Closing previous instance!!");
			instance.close();
			try { Thread.sleep(25); } catch (Throwable t) { }
		}
		
		this.drumkit = drumkit;
		this.portname = portname;
		cmd = shellCommand + drumkit.getAbsolutePath();
		process = Runtime.getRuntime().exec(cmd);

	    Jack jack = Jack.getInstance();
	    try { // wait a bit for drumkv1 to create its ports
	    	while (jack.getPorts(OUT_R, null, null).length == 0 || jack.getPorts(MIDI_IN, null, null).length == 0) {
		    	Thread.sleep(50); }
	    } catch (InterruptedException e) { }

		instance = this;

 		log.info("drumkv1 process created");
 		if (makeConnections) makeConnections();
	}

	public Drumkv1() throws IOException, JackException {
		this(defaultDrumkit, JudahPort.DRUMS.getPortName(), false);
	}
	
	@SuppressWarnings("deprecation")
	public void makeConnections() throws IOException, JackException{
		Jack jack = Jack.getInstance();
		String[] from = jack.getPorts(portname, JackPortType.MIDI, EnumSet.of(JackPortIsOutput));
		
		String[] to = jack.getPorts("in", JackPortType.MIDI, EnumSet.of(JackPortIsInput));
		if (from.length == 1 && to.length > 0) {
			for (String s : to)
				if (s.contains("drumkv1")) {
					jack.connect(from[0], s);
					break;
				}
		}
		String[] out1 = jack.getPorts("out_1", JackPortType.AUDIO, EnumSet.of(JackPortIsOutput));
		String[] out2 = jack.getPorts("out_2", JackPortType.AUDIO, EnumSet.of(JackPortIsOutput));
		if (out1.length < 1 || out2.length < 1) 
			log.warn("Danger Will Robinson. " + Arrays.toString(out1) + " and " + Arrays.toString(out2));
		
		for (String s : out1)
			if (s.contains("drumkv1"))
				jack.connect(s, "system:playback_1");
		for (String s : out2)
			if (s.contains("drumkv1"))
				jack.connect(s, "system:playback_2");
	}

	public void close() {
		if (process != null)
			process.destroy();	
		process = null;
		drumkit = null;
		instance = null;
	}

//	@Override
//	public String getServiceName() {
//		return null;
//	}
//	@Override public List<Command> getCommands() { return Collections.emptyList(); }
//	@Override public void execute(Command cmd, HashMap<String, Object> props) throws Exception { }
//	@Override public Tab getGui() { return null; }


	
}
