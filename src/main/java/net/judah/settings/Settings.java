package net.judah.settings;

import static net.judah.jack.AudioTools.*;
import static net.judah.looper.LoopInterface.CMD.*;
import static net.judah.settings.Command.*;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.sound.midi.InvalidMidiDataException;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.fluid.FluidSynth;
import net.judah.instruments.MPK;
import net.judah.looper.Loop;
import net.judah.looper.old.LoopSettings;
import net.judah.midi.Midi;
import net.judah.mixer.MixerCommands;
import net.judah.mixer.MixerPort.PortDescriptor;
import net.judah.mixer.MixerPort.Type;
import net.judah.util.Tab;

@Log4j
public class Settings implements Service, Serializable {
	private static final long serialVersionUID = 1153987490195804305L;

	
	public static final File DEFAULT_CONF_FILE = new File(System.getProperty("user.home"), "JudahZone.conf");
	public static final Settings DEFAULT_SETTINGS = createCarlaSettings();
	
	static final String[] SECTIONS = {"props", "mappings", "patchbay", "loops"};

	
	
	@Getter private final Properties props;
	@Getter private final Patchbay patchbay;
	@Getter private final List<Mapping> mappings;

	private transient Settings deserialized; // for dirty check

	Settings(Settings other) {
		props = new Properties();
		props.putAll(other.props);
		patchbay = new Patchbay(other.patchbay);
		mappings = new ArrayList<>();
		mappings.addAll(other.mappings);

	}

//	/** empty settings */
//	public Settings() {
//		props = new Properties();
//		// commands = new ArrayList<Command>();
//		mappings = new ArrayList<Mapping>();
//		patchbay = new Patchbay("dummy", Collections.e);
//		loops = new ArrayList<LoopSettings>();
//	}

	public Settings(Properties props, Patchbay patchbay, List<Mapping> mappings) {
		if (props == null) throw new NullPointerException("props");
		this.props = props;
		this.mappings = mappings;
		this.patchbay = patchbay;
	}

	public boolean isDirty() {
		return deserialized == null || !this.equals(deserialized);
	}

	@Override public int hashCode() {
		return props.hashCode() + mappings.hashCode() + patchbay.hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (obj.getClass() != this.getClass()) return false;
		Settings other = (Settings)obj;
		return props.equals(other.props) && mappings.equals(other.mappings) && patchbay.equals(other.patchbay);
	}

	/** saves settings to supplied file */
	public void serialize(File file) throws IOException {
		FileOutputStream stream = new FileOutputStream(file);
		ObjectOutputStream out = new ObjectOutputStream(stream);
		out.writeObject(this);
		out.close();
		log.info("Wrote configuration to " + file.getAbsolutePath());
	}

	public static Settings deserialize(File file) throws IOException, ClassNotFoundException {
		FileInputStream stream = new FileInputStream(file);
		ObjectInputStream in = new ObjectInputStream(stream);
		Object o = in.readObject();
		in.close();
		if (o instanceof Settings == false)
			throw new ClassNotFoundException(o.getClass().getCanonicalName() + " for " + file.getAbsolutePath());
		Settings settings = (Settings)o;
		settings.deserialized = settings;
		log.info("Read configuration from " + file.getAbsolutePath());
		settings.deserialized = new Settings(settings);
		return settings;
	}

	@SuppressWarnings("unused")
	private static void testSerialization() throws IOException, ClassNotFoundException {

		Properties props1 = new Properties();
		props1.put("name", "FIRST");
		Properties props2 = new Properties();
		props2.put("name", "TWO");
		Service service1 = new Service() {
			@Override public String getServiceName() { return "1"; }
			@Override public ArrayList<Command> getCommands() { return null; }
			@Override public void execute(Command cmd, Properties props) throws Exception { }
			@Override public boolean equals(Object obj) { return obj instanceof Service ? ((Service)obj).getServiceName().equals(getServiceName()) : false; }
			@Override public int hashCode() { return getServiceName().hashCode(); }
			@Override public void close() {		}
			@Override public Tab getGui() { return null; }};
		Service service2 = new Service() {
			@Override public String getServiceName() { return "2"; }
			@Override public ArrayList<Command> getCommands() { return null; }
			@Override public void execute(Command cmd, Properties props) throws Exception { }
			@Override public boolean equals(Object obj) { return obj instanceof Service ? ((Service)obj).getServiceName().equals(getServiceName()) : false; }
			@Override public int hashCode() { return getServiceName().hashCode(); }
			@Override public void close() { }
			@Override public Tab getGui() { return null; }};
		Command command1 = new Command( "Command 1", service1, null, "Interesting");
		Command command2 = new Command("Command 2", service2, null, "Not so interesting");
		Command command3 = new Command("Command 3", service1, null, "boring feature");
		Command command4 = new Command("Command 4", service2, null, "The bomb.");
		ArrayList<Command> commands1 = new ArrayList<>();
		commands1.add(command1);
		commands1.add(command2);
		ArrayList<Command> commands2 = new ArrayList<>();
		commands2.add(command3);
		commands2.add(command4);

		File user = new File(System.getProperty("user.dir"));
		File file1 = new File(user, "ONE.conf");
		File file2 = new File(user, "TWO.conf");

		// TODO mappings and patchbay
		Settings write1 = new Settings(props1, null, null);
		write1.serialize(file1);

		Settings write2 = new Settings(props2, null, null);
		write2.serialize(file2);

		Settings read1 = deserialize(file1);
		Settings read2 = deserialize(file2);

		if (!write1.equals(read1)) {
			throw new IOException("Ooops: " + write1.props.getProperty("name") + " vs. " + read1.props.getProperty("name"));
		}
		if (!write1.equals(read1)) {
			throw new IOException("Ooops: " + write2.props.getProperty("name") + " vs. " + read2.props.getProperty("name"));
		}

		assert file1.exists() : file1.getAbsolutePath();
		assert file2.exists() : file2.getAbsolutePath();
		System.out.println(file1.getAbsolutePath() + " and " + file2.getAbsolutePath());
		// PASS
	}

	public String get(String key) {
		return props.getProperty(key);
	}
	public String get(String key, String backup) {
		return props.getProperty(key, backup);
	}

	private static Settings createCarlaSettings() {
		final String client = JudahZone.JUDAHZONE;
		
		List<PortDescriptor> ports = new ArrayList<>();
		List<Patch> connections = new ArrayList<>();
		
		PortDescriptor in, out;
		
		// Inputs
		in = new PortDescriptor("guitar_left", Type.LEFT, AUDIO, JackPortIsInput);
		ports.add(in);
		in = new PortDescriptor("guitar_right", Type.RIGHT, AUDIO, JackPortIsInput);
		ports.add(in);
		
		in = new PortDescriptor("mic_left", Type.LEFT, AUDIO, JackPortIsInput);
		ports.add(in);
		in = new PortDescriptor("mic_right", Type.RIGHT, AUDIO, JackPortIsInput);
		ports.add(in);
		
		in = new PortDescriptor("drums_left", Type.LEFT, AUDIO, JackPortIsInput);
		ports.add(in);
		in = new PortDescriptor("drums_right", Type.RIGHT, AUDIO, JackPortIsInput);
		ports.add(in);
		
		in = new PortDescriptor("synth_left", Type.LEFT, AUDIO, JackPortIsInput);
		ports.add(in);
//		connections.add(new Patch(FluidSynth.LEFT_PORT, portName(client, in.getName())));
		in = new PortDescriptor("synth_right", Type.RIGHT, AUDIO, JackPortIsInput);
		ports.add(in);
//		connections.add(new Patch(FluidSynth.RIGHT_PORT, portName(client, in.getName())));
		
		// Outputs
		out = new PortDescriptor("left", Type.LEFT, AUDIO, JackPortIsOutput);
		ports.add(out);
		connections.add(new Patch(portName(client, out.getName()), "system:playback_1"));
		
		out = new PortDescriptor("right", Type.RIGHT, AUDIO, JackPortIsOutput);
		ports.add(out);
		connections.add(new Patch(portName(client, out.getName()), "system:playback_2"));
		
		
		List<LoopSettings> loops = new ArrayList<>();
		//		//	loops.add(new LoopSettings("Master", MasterLoop.class));
		//		//	loops.add(new LoopSettings("Slave", SlaveLoop.class));
		//		for (LoopSettings loop : loops) {
		//			connectOut(portName(client, loop.getName()), connections);
		//		}
		
		Patchbay patches = new Patchbay(client, ports, loops, connections);
		Settings result = new Settings(new Properties(), patches, new ArrayList<Mapping>());
		
		return result;		
	}
	
	@SuppressWarnings("unused")
	private static Settings createDefaults() {
		final String client = JudahZone.JUDAHZONE;
		
		List<PortDescriptor> ports = new ArrayList<>();
		List<Patch> connections = new ArrayList<>();
		
		PortDescriptor in, out;
		
		// Inputs
		in = new PortDescriptor("guitar_in", Type.MONO, AUDIO, JackPortIsInput);
		ports.add(in);
		connections.add(new Patch("system:capture_1", portName(client, in.getName())));
		in = new PortDescriptor("mic_in", Type.MONO, AUDIO, JackPortIsInput);
		ports.add(in);
		connections.add(new Patch("system:capture_2", portName(client, in.getName())));
		in = new PortDescriptor("drums_in", Type.MONO, AUDIO, JackPortIsInput);
		ports.add(in);
		connections.add(new Patch("system:capture_4", portName(client, in.getName())));
		in = new PortDescriptor("synth_left", Type.LEFT, AUDIO, JackPortIsInput);
		ports.add(in);
		connections.add(new Patch(FluidSynth.LEFT_PORT, portName(client, in.getName())));
		in = new PortDescriptor("synth_right", Type.RIGHT, AUDIO, JackPortIsInput);
		ports.add(in);
		connections.add(new Patch(FluidSynth.RIGHT_PORT, portName(client, in.getName())));
		ports.add(new PortDescriptor("aux_in", Type.MONO, AUDIO, JackPortIsInput)); // aux not connected yet  
		
		// Outputs
		out = new PortDescriptor("guitar_out", Type.MONO, AUDIO, JackPortIsOutput);
		ports.add(out);
		connectOut(portName(client, out.getName()), connections);		
		
		out = new PortDescriptor("mic_out", Type.MONO, AUDIO, JackPortIsOutput);
		ports.add(out);
		connectOut(portName(client, out.getName()), connections);
		
		out = new PortDescriptor("drums_out", Type.MONO, AUDIO, JackPortIsOutput);
		ports.add(out);
		connectOut(portName(client, out.getName()), connections);

		out = new PortDescriptor("synthL_out", Type.LEFT, AUDIO, JackPortIsOutput);
		ports.add(out);
		connections.add(new Patch(portName(client, out.getName()), "system:playback_1"));
		
		out = new PortDescriptor("synthR_out", Type.RIGHT, AUDIO, JackPortIsOutput);
		ports.add(out);
		connections.add(new Patch(portName(client, out.getName()), "system:playback_2"));

		ports.add(new PortDescriptor("aux_out", Type.MONO, AUDIO, JackPortIsOutput)); // aux not connected yet
		
		List<LoopSettings> loops = new ArrayList<>();
		loops.add(new LoopSettings("Master", Loop.class));
//		loops.add(new LoopSettings("Slave", SlaveLoop.class));
		
//		for (LoopSettings loop : loops) {
//			connectOut(portName(client, loop.getName()), connections);
//		}
		
		Patchbay patches = new Patchbay(client, ports, loops, connections);
		Settings result = new Settings(new Properties(), patches, new ArrayList<Mapping>());
		
		return result;
	}

	@Override
	public void close() {
		if (isDirty())
			try {
				log.warn("NOT SAVING SETTINGS");
//				settings.serialize(judah.getSettingsFile());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		else
			log.debug("No new settings to save");
	}

	public static List<Mapping> dummyMappings(List<Command> commands) {
		if (commands.isEmpty()) assert false;
		List<Mapping> mappings = new ArrayList<>();
		Properties props;
		for (Command command : commands) {
			try {
				//	if (command.getName().equals("EffectOn")) {
				//		mappings.add(new Mapping(command, new Midi(176, 0, 101, 127), null));
				//	} else if (command.getName().equals("EffectOff")) {
				//		mappings.add(new Mapping(command, new Midi(176, 0, 101, 0), null));
				if (command.getName().equals(PLAY.getLabel())) {
					props = new Properties();
					props.put("Loop", 1);
					props.put("Active", true);
					mappings.add(new Mapping(command, new Midi(176, 0, 100, 127), props));
	
					props = new Properties();
					props.put("Loop", 1);
					props.put("Active", false);
					mappings.add(new Mapping(command, new Midi(176, 0, 100, 0), props));
	
					props = new Properties();
					props.put("Loop", 0);
					props.put("Active", true);
					mappings.add(new Mapping(command, new Midi(176, 0, 99, 127), props));
	
					props = new Properties();
					props.put("Loop", 0);
					props.put("Active", false);
					mappings.add(new Mapping(command, new Midi(176, 0, 99, 0), props));
	
				} else if (command.getName().equals(CLEAR.getLabel())) {
					mappings.add(new Mapping(command, new Midi(176, 0, 98, 127), null));
					// broken on foot pedal device: mapping.add(new Mapping(command, new Midi(176, 0, 98, 0), null));
	
				} else if (command.getName().equals(RECORD.getLabel())) {
					props = new Properties();
					props.put("Loop", 1);
					props.put("Active", true);
					mappings.add(new Mapping(command, new Midi(176, 0, 97, 127), props));
	
					props = new Properties();
					props.put("Loop", 1);
					props.put("Active", false);
					mappings.add(new Mapping(command, new Midi(176, 0, 97, 0), props));
	
					props = new Properties();
					props.put("Loop", 0);
					props.put("Active", true);
					mappings.add(new Mapping(command, new Midi(176, 0, 96, 127), props));
	
					props = new Properties();
					props.put("Loop", 0);
					props.put("Active", false);
					mappings.add(new Mapping(command, new Midi(176, 0, 96, 0), props));
				} else if (command.getName().equals("tick")) {
					mappings.add(new Mapping(command, new Midi(176, 0, 101, 127), null));
				} else if (command.getName().equals("tock")) {
					mappings.add(new Mapping(command, new Midi(176, 0, 101, 0), null));
				} else if (command.getName().equals("Metronome settings")) {
					props = new Properties();
					props.put("bpm", "todo");
					mappings.add(new Mapping(command, MPK.knob(0, 0), props, DYNAMIC));
					props = new Properties();
					props.put("volume", "todo");
					mappings.add(new Mapping(command, MPK.knob(0, 1), props, DYNAMIC));
				} else if (command.getName().equals(MixerCommands.GAIN_COMMAND)) {
//					props = new Properties();
//					props.put(MixerCommands.GAIN_PROP, "todo");
//					props.put(MixerCommands.CHANNEL_PROP, 0);
//					mappings.add(new Mapping(command, MPK.knob(0, 4), props, DYNAMIC));
//					props = new Properties();
//					props.put(MixerCommands.GAIN_PROP, "todo");
//					props.put(MixerCommands.CHANNEL_PROP, 1);
//					mappings.add(new Mapping(command, MPK.knob(0, 5), props, DYNAMIC));
//					props = new Properties();
//					props.put(MixerCommands.GAIN_PROP, "todo");
//					props.put(MixerCommands.CHANNEL_PROP, 2);
//					mappings.add(new Mapping(command, MPK.knob(0, 6), props, DYNAMIC));
//					props = new Properties();
//					props.put(MixerCommands.GAIN_PROP, "todo");
//					props.put(MixerCommands.CHANNEL_PROP, 3);
//					mappings.add(new Mapping(command, MPK.knob(0, 7), props, DYNAMIC));
//					props = new Properties(); // master volume
//					props.put(Mixer.GAIN_PROP, "todo");
//					props.put(Mixer.CHANNEL_PROP, -1);
//					mappings.add(new Mapping(command, new Midi(176, 0, 17), props, DYNAMIC));

				} else if (command.getName().equals(MixerCommands.PLUGIN_COMMAND)) {
					props = new Properties();
					props.put(MixerCommands.CHANNEL_PROP, -1);
					props.put(MixerCommands.PLUGIN_PROP, "ZynPhaser");
					mappings.add(new Mapping(command, new Midi(176, 9, 32), props, DYNAMIC));
					/*	 on:  Ch 10, Ctrl  32, Val  56  
						off:  Ch 10, Ctrl  32, Val   0 
						 on:  Ch 10, Ctrl  32, Val 127  */
				}
			} catch(InvalidMidiDataException e) {
				e.printStackTrace();
			}
		}
		log.info("Loaded " + mappings.size() + " dummy mappings.");
		return mappings;
	}
	
	private static void connectOut(String portName, List<Patch> connections) {
		connections.add(new Patch(portName, "system:playback_1"));
		connections.add(new Patch(portName, "system:playback_2"));
	}

	@Override
	public String getServiceName() {
		return Settings.class.getSimpleName(); 
	}
	

	@Override
	public List<Command> getCommands() {
		return Collections.emptyList(); // TODO
	}

	@Override
	public void execute(Command cmd, Properties props) throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public Tab getGui() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
