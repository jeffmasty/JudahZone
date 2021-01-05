package net.judah.fluid;

import static net.judah.settings.Commands.SynthLbls.*;
import static net.judah.util.Constants.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackException;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.api.Service;
import net.judah.midi.JudahMidi;
import net.judah.midi.ProgMsg;
import net.judah.util.Console;
import net.judah.util.JudahException;


/** runs and then connects to FluidSynth stdin and stdout ports, connects midi and speakers */
@Log4j
public class FluidSynth implements Service {

	@Getter private static FluidSynth instance;
	
	public static final String LEFT_PORT = "fluidsynth-midi:left"; // "fluidsynth:l_00";
	public static final String RIGHT_PORT = "fluidsynth-midi:right"; // "fluidsynth:r_00";
	public static final String MIDI_PORT = "fluidsynth-midi:midi_00"; // "fluidsynth:midi";

    public static final File SOUND_FONT = new File("/usr/share/sounds/sf2/FluidR3_GM.sf2"); // "/usr/share/sounds/sf2/JJazzLab-SoundFont.sf2"
    
    /** Drums midi channel */
    private static final int DRUMS = 9;
    
	final String MIDI_DRIVER = "jack";
	final String AUDIO_DRIVER = "jack";

	private final String shellCommand;
	private Process process;

	private FluidListener listener;
	/** talks to FluidSynth on it's stdin */
    private OutputStream outStream;
    
    @Getter private final FluidConsole console;
    
	@Getter private Instruments instruments = new Instruments();
	@Getter private Channels channels = new Channels();

	private final Command progChange, instUp, instDown, drumBank, direct;
	@Getter private final List<Command> commands;
	
	private float gain = 2.5f; // max 5
	private float reverbLevel = 0.75f;
	private float roomSize = 0.75f;
	private float dampness = 0.75f;

	public FluidSynth (int sampleRate, boolean startListeners) throws JackException, JudahException, IOException {
		this(sampleRate, SOUND_FONT, startListeners);
	}
	
	public FluidSynth (int sampleRate) throws JackException, JudahException, IOException {
		this(sampleRate, SOUND_FONT, true);
	}
	
	@SuppressWarnings("deprecation")
	public FluidSynth (int sampleRate, File soundFont, boolean startListeners) throws JackException, JudahException, IOException {
	    instance = this;
		shellCommand = "fluidsynth" +
				" --midi-driver=jack --audio-driver=jack" +
	    		" -o synth.ladspa.active=0  --sample-rate " + sampleRate + " " +
				SOUND_FONT.getAbsolutePath();
		
		console = new FluidConsole(this);
		log.debug(shellCommand);
		try {
			process = Runtime.getRuntime().exec(shellCommand);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}

	    // read FluidSynth output
	    listener = new FluidListener(process.getInputStream(), false);
	    new FluidListener(process.getErrorStream(), true).start();
	    if (startListeners)
	    	listener.start();
	    outStream = process.getOutputStream();

	    Jack jack = Jack.getInstance();
	    try { // wait for fluid synth to init
	    	while (jack.getPorts(LEFT_PORT, null, null).length == 0) {
		    	Thread.sleep(50);
	    	}
	    	gain(gain);
	    	Thread.sleep(40);
//	    	connect(midi.getJackclient(), midi.getSynth());
//	    	connect(midi.getJackclient(), midi.getDrums());
//	    	Jack.getInstance().connect(LEFT_PORT, "system:playback_1");
//			Jack.getInstance().connect(RIGHT_PORT, "system:playback_2");
	    	
	    } catch (InterruptedException e) {
	    	log.error(e);
	    }
	    if (startListeners)
	    	sync();
		initReverb();
		HashMap<String, Class<?>> template = new HashMap<String, Class<?>>();
		template.put("channel", Integer.class);
		template.put("preset", Integer.class);
		progChange = new Command(PROGCHANGE.name, PROGCHANGE.desc, template) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
					int channel = 0;
					try {
						channel = Integer.parseInt(props.get("channel").toString());
					} catch (Throwable t) { 
						log.debug(t.getMessage()); }
					JudahMidi.getInstance().queue(progChange(channel, Integer.parseInt(props.get("preset").toString())));
		}};
		
		instUp = new Command(INSTUP.name, INSTUP.desc) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				instUp(0, true);
			}};
		instDown = new Command(INSTDOWN.name, INSTDOWN.desc) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				instUp(0, false);
			}};
		template = new HashMap<String, Class<?>>();
		template.put("up", Boolean.class);
		template.put("preset", Integer.class);
		drumBank = new Command(DRUMBANK.name, DRUMBANK.desc, template) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				
				Integer preset = null;
				try {
					preset = Integer.parseInt("" + props.get("preset"));
					sendCommand(FluidCommand.PROG_CHANGE, "9 " + preset);
					return;
				} catch (Throwable t) { }
				
				try {
					boolean up = Boolean.parseBoolean("" + props.get("up"));
					instUp(DRUMS, up);
				} catch (Throwable t) { }
			}
		};
		template = new HashMap<String, Class<?>>();
		template.put("string", String.class);
		direct = new Command(DIRECT.name, DIRECT.desc, template) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				String[] split = props.get("string").toString().split(";");
				for (String cmd : split)
					sendCommand(cmd);
			}
		};

	    commands = Arrays.asList(new Command[] {progChange, instUp, instDown, drumBank, direct});				
		// doHelp();
		Console.addText( "FluidSynth channels: " + channels.size() + " instruments: " + instruments.size());
	}

	private void syncChannels() throws InterruptedException, IOException, JudahException {
		listener.sysOverride(FluidCommand.CHANNELS);
		outStream.write( (FluidCommand.CHANNELS.code + NL).getBytes() );
		outStream.flush();
		int count = 0;
		while (listener.sysOverride != null && count++ < 15) {
			Thread.sleep(30);
		}
		if (listener.channels.isEmpty())
			throw new JudahException("Error reading channels");
		else {
			channels.clear();
			for (FluidChannel c : listener.channels)
				channels.add(c);
		}
	}

	private void syncInstruments() throws InterruptedException, IOException, JudahException {
		listener.sysOverride(FluidCommand.INST);
		outStream.write((FluidCommand.INST.code + "1" + NL).getBytes());
		outStream.flush();
		int count = 0;
		while (listener.sysOverride != null && count++ < 20) {
			Thread.sleep(30);
		}
		if (listener.instruments.isEmpty())
			throw new JudahException("Fluid Error reading instruments");
		instruments.clear();
		for (FluidInstrument i : listener.instruments)
			instruments.add(i);
	}

	public void sync() {
		try {
			syncChannels();
			syncInstruments();
		} catch (Throwable t) {
			Console.addText("sync failed. " + t.getMessage());
		}

	}

	private void initReverb() {
		sendCommand("chorus off");
		sendCommand("reverb on");
		reverb(reverbLevel);
		roomSize(roomSize);
		dampness(dampness);
	}

	public Midi bankUp() {
		try {
			return new Midi(ShortMessage.CONTROL_CHANGE, 0, 0, 3);
		} catch (InvalidMidiDataException e) {
			log.error(e);
		}
		return null;
	}

	public Midi bankDown() {
		try {
			return new Midi(ShortMessage.CONTROL_CHANGE, 0, 0, 2);
		} catch (InvalidMidiDataException e) {
			log.error(e);
		}
		return null;
	}

	/** MIDI
	The MIDI message used to specify the instrument is called a "program change" message. It has one STATUS byte and one DATA byte :
Status byte : 1100 CCCC
Data byte 1 : 0XXX XXXX
where CCCC is the MIDI channel (0 to 15) and XXXXXXX is the instrument number from 0 to 127. */

	public Midi progChange(final int channel, int preset) {
		final int before = channels.getCurrentPreset(channel);
		try {
			if (++preset == 129)
			preset = 1; 
				
			final Midi msg = new ProgMsg(channel, preset);

			new Thread() {
				@Override public void run() {
					try {
						Thread.sleep(22);
						syncChannels();
						Thread.sleep(40);
						int after = channels.getCurrentPreset(channel);
						Console.info("PROG CHNG " + instruments.get(after) + " (from " + instruments.get(before) + ")");
					} catch (Throwable e) { log.warn(e.getMessage(), e); }
				};
			}.start();

			return msg;
		} catch (Throwable t) {
			Console.warn(t.getMessage(), t);
			return null;
		}
	}
	
	public void instUp(int channel, boolean up) {
		int current = channels.getCurrentPreset(channel);
		int bank = channels.getBank(channel);
		int preset = instruments.getNextPreset(bank, current, up);
		progChangeConsole(channel, preset);
	}
	
	public int progChangeConsole(int channel, int preset) {
		try {
			String progChange = FluidCommand.PROG_CHANGE.code + channel + " " + preset;
			sendCommand(progChange);
			syncChannels();
			int result = channels.getCurrentPreset(channel);
			int bank = channels.getBank(channel);
			for (FluidInstrument f : instruments) 
				if (f.group == bank && f.index == preset) 
					Console.addText(f.toString());
			return result;
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			return -1;
		}
	}

	public void sendCommand(FluidCommand cmd) throws JudahException {
		if (cmd.type != ValueType.NONE) {
			throw new JudahException(cmd + " Command requires a value");
		}
		sendCommand(cmd.code);
	}

	private void sendCommand(FluidCommand cmd, Object value) {
		String send = cmd.code + value;
		sendCommand(send);
	}

	public void sendCommand(String string) {
		log.trace("sendCommand: " + string);
		if (false == string.endsWith(NL))
			string = string + NL;

		try {
		    outStream.write((string).getBytes());
		    outStream.flush();
		} catch(IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	void mute() {
		sendCommand(FluidCommand.GAIN, FluidCommand.GAIN.min);
	}

	void maxGain() {
		sendCommand(FluidCommand.GAIN, FluidCommand.GAIN.max);
	}

	public void gain(float value) {
		if (value < 0) value = 0;
		if (value > 5) value = 5;
		sendCommand(FluidCommand.GAIN, value);
		this.gain = value;
	}

	public void reverb(float val) {
		if (val > 1) val = 1;
		if (val < 0) val = 0;
		sendCommand(FluidCommand.REVERB, val);
		this.reverbLevel = val;
	}

	public void roomSize(float val) {
		if (val > 1) val = 1;
		if (val < 0) val = 0;
		sendCommand(FluidCommand.ROOM_SIZE, val);
		this.roomSize = val;
	}

	public void dampness(float val) {
		if (val > 1) val = 1;
		if (val < 0) val = 0;
		sendCommand(FluidCommand.DAMPNESS, val);
		this.dampness = val;
	}

	// TODO chorus routines to MIDI "SF2 default modulators" (too many jack hangs on stdin)
	public void chorusDelayLines(int val) {
		FluidCommand delay = FluidCommand.CHORUS_DELAY_LINES;
		if (val > delay.max.intValue()) val = delay.max.intValue();
		if (val < delay.min.intValue()) val = delay.min.intValue();
		sendCommand(delay.code + val);
	}

	public void chorusLevel(float val) {
		FluidCommand level = FluidCommand.CHORUS_OUTPUT;
		if (val > level.max.floatValue()) val = level.max.floatValue();
		if (val < level.min.intValue()) val = level.min.intValue();
		sendCommand(level.code + val);
	}

	public void chorusSpeed(float val) {
		FluidCommand speed = FluidCommand.CHORUS_SPEED;
		if (val > speed.max.floatValue()) val = speed.max.floatValue();
		if (val < speed.min.floatValue()) val = speed.min.floatValue();
		sendCommand(speed.code + val);
	}

	public void chorusDepth(int val) {
		FluidCommand depth = FluidCommand.CHORUS_DEPTH;
		if (val > depth.max.intValue()) val = depth.max.intValue();
		if (val < depth.min.intValue()) val = depth.min.intValue();
		sendCommand(depth.code + val);
	}

	public Midi preset(int val) {
		// TODO store in a Set
		if (val == 1) {
			return progChange(0, 19); // 19 church organ
		}
		if (val == 2) {
			return progChange(0, 24); // 24 nylon guitar
		}
		return null;
	}

	@Override
	public void close() {
		try {
  	  		sendCommand(FluidCommand.QUIT);
  	  	} catch (JudahException e) { log.warn(e); }
	}

//	public void connect(JackClient jackclient, JackPort port) throws JackException {
//		log.warn("Trying to connect " + port.getName() +" to " + MIDI_PORT);
//	    Jack.getInstance().connect(jackclient, port.getName(), MIDI_PORT);
//	}


	public static class Channels extends ArrayList<FluidChannel> {
		/** @return preset instrument index for the channel */
		public int getCurrentPreset(int channel) {
			for (FluidChannel c : this) 
				if (c.channel == channel) 
					return c.preset;
			return -1;
		}
		public int getBank(int channel) {
			for (FluidChannel c : this)
				if (c.channel == channel)
					return c.bank;
			return -1;
		}
	}

	
	
	public static class Instruments extends ArrayList<FluidInstrument> {
		public int getNextPreset(int bank, int current, boolean up) {
			int index = -1;
			int count = -1;
			// extract bank
			ArrayList<FluidInstrument> roll = new ArrayList<FluidInstrument>();
			for (FluidInstrument i : this) 
				if (i.group == bank) {   
					count++;
					roll.add(i);
					if (i.index == current) {
						index = count;
					}
				}
			if (index == -1) 
				throw new IndexOutOfBoundsException("preset " + current + " for bank " + bank + ": " + Arrays.toString(roll.toArray()));
			if (up) {
				index++;
				if (index == roll.size())
					index = 0;
				return roll.get(index).index;
			}
			else {
				index--;
				if (index < 0)
					index = roll.size() -1;
				return roll.get(index).index;
			}
		}
	}

	@Override
	public void properties(HashMap<String, Object> props) {
		if (props.containsKey("fluid")) {
			String[] split = props.get("fluid").toString().split(";");
			for (String cmd : split)
				sendCommand(cmd);
		}
	}
	
}

// sendCommand( on ? "reverb on" : "reverb off");
// sendCommand( on ? "chorus on" : "chorus off");
