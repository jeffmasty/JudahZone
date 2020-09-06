package net.judah.fluid;

import static net.judah.Constants.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import lombok.extern.log4j.Log4j;
import net.judah.Constants;
import net.judah.Constants.Toggles;
import net.judah.JudahException;
import net.judah.Tab;
import net.judah.midi.Midi;
import net.judah.midi.MidiClient;
import net.judah.midi.ProgMsg;
import net.judah.settings.Command;
import net.judah.settings.Service;;

/** runs and then connects to FluidSynth stdin and stdout ports, connects midi and speakers */
@Log4j
public class FluidSynth implements Service {

	public static final String LEFT_PORT = "fluidsynth:l_00";
	public static final String RIGHT_PORT = "fluidsynth:r_00";
	public static final String MIDI_PORT = "fluidsynth:midi";

    public static final File SOUND_FONT = new File("/usr/share/sounds/sf2/FluidR3_GM.sf2"); // fluid-soundfount-gm package, 150mb
	
	final String MIDI_DRIVER = "jack";
	final String AUDIO_DRIVER = "jack";

	private final String shellCommand;
	private Process process;

	private FluidListener listener;
	/** talks to FluidSynth on it's stdin */
    private OutputStream console;
    private FluidUI fluidWindow;

	Instruments instruments = new Instruments();
	Channels channels = new Channels();

	//*--- serialize ---*//

	private float gain = 0.7f; // max 5
	private boolean reverb; // toggles on in init()
	private boolean chorus; // toggles on in init()
	private float reverbLevel = 0.75f;
	private float roomSize = 0.75f;
	private float dampness = 0.75f;

	public FluidSynth (MidiClient midi) throws JackException, JudahException, IOException, JackException {
		this(midi, SOUND_FONT);
	}
	
	@SuppressWarnings("deprecation")
	public FluidSynth (MidiClient midi, File soundFont) throws JackException, JudahException, IOException, JackException {
		shellCommand = "fluidsynth" +
				" --midi-driver=jack --audio-driver=jack" +
	    		" -o synth.ladspa.active=0  --sample-rate " + Constants.SAMPLE_RATE + " " +
				SOUND_FONT.getAbsolutePath();
		
		fluidWindow = new FluidUI(this);
		log.debug(shellCommand);
		try {
			process = Runtime.getRuntime().exec(shellCommand);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}

	    // read FluidSynth output
	    listener = new FluidListener(fluidWindow, process.getInputStream(), false);
	    new FluidListener(fluidWindow, process.getErrorStream(), true).start();
	    listener.start();
	    console = process.getOutputStream();
	    assert console != null;

	    Jack jack = Jack.getInstance();
	    try { // wait for fluid synth to init
	    	while (jack.getPorts(LEFT_PORT, null, null).length == 0) {
		    	Thread.sleep(50);
	    	}
	    	gain(gain);
	    	Thread.sleep(30);
	    	connect(midi.getJackclient(), midi.getSynth());
	    } catch (InterruptedException e) {
	    	log.error(e);
	    }
	    sync();

		fluidWindow.addText( "channels: " + channels.size() + " instruments: " + instruments.size());
		initReverb();
		sendCommand("help");
		//	try { new FluidLadspa(this, window);
		//	} catch (JudahException e) { log.error("FLUID LADSPA Failed.", e); }
	}


	private void syncChannels() throws InterruptedException, IOException, JudahException {
		listener.sysOverride(FluidCommand.CHANNELS);
		console.write( (FluidCommand.CHANNELS.code + NL).getBytes() );
		console.flush();
		int count = 0;
		while (listener.sysOverride != null && count++ < 20) {
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
		console.write((FluidCommand.INST.code + "1" + NL).getBytes());
		console.flush();
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

	private void sync() {
		try {
			syncChannels();
			syncInstruments();
		} catch (Throwable t) {
			fluidWindow.addText("sync failed. " + t.getMessage() + NL);
			log.error(t.getMessage(), t);
		}

	}

	public void judahCommand(String text) {
		if (text.equals("sync"))
			sync();

		if (text.equals("channels")) {
			fluidWindow.addText("channels: " + channels.size() + NL);
			for (FluidChannel channel : channels) {
				fluidWindow.addText(channel.toString() + NL);
			}
			fluidWindow.newLine();
		}
		if (text.equals("inst") || text.equals("instruments")) {
			fluidWindow.addText("instruments: " + instruments.size() + NL);
			for (FluidInstrument instrument : instruments) {
				fluidWindow.addText(instrument.toString() + NL);
			}
			fluidWindow.newLine();
		}
		if (text.equals("current")) {
			fluidWindow.addText("current: " + channels.get(channels.getCurrentPreset(0)));
		}
		if (text.equals("mute")) {
			mute();
		}
		if (text.equals("maxGain")) {
			maxGain();
		}
	}
	public void userCommand(String text) {
		if (text == null) return;
		if (text.startsWith("judah ")) {
			judahCommand(text.replace("judah ", "").trim());
			return;
		}
		sendCommand(text);
	}

	private void initReverb() {
		//		toggleReverb();
		//		toggleChorus();
		reverb(reverbLevel);
		roomSize(roomSize);
		dampness(dampness);
		sendCommand("reverb off");
		sendCommand("chorus off");
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

	public Midi progChange(final int channel, final int preset) {
		final int before = channels.getCurrentPreset(channel);
		try {
			final Midi msg = new ProgMsg(channel, preset);

			new Thread() {
				@Override public void run() {
					try {
						Thread.sleep(10);
						syncChannels();
						Thread.sleep(40);
						int after = channels.getCurrentPreset(channel);
						fluidWindow.addText(instruments.get(after) + " (from " + instruments.get(before) + ")");
					} catch (Throwable e) { log.warn(e.getMessage(), e); }
				};
			}.start();

			return msg;
		} catch (Throwable t) {
			fluidWindow.addText(t.getMessage());
			log.error(t);
			return null;
		}
	}
	public int progChangeConsole(int channel, int preset) {
		try {
			String progChange = FluidCommand.PROG_CHANGE.code + channel + " " + preset;
			sendCommand(progChange);
			syncChannels();
			int result = channels.getCurrentPreset(channel);
			fluidWindow.addText(channels.get(channels.getCurrentPreset(0)).toString());
			return result;
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			return -1;
		}
	}

	public Midi instrumentUp(int channel, int bank) {
		int current = channels.getCurrentPreset(channel);
		int max = instruments.getMaxPreset(bank);
		if (current == max)
			current = 0;
		else
			current++;
		return progChange(channel, current);
	}

	public Midi instrumentUp() {
		return instrumentUp(0, 0);
	}

	public Midi instrumentDown(int channel, int bank) {
		int current = channels.getCurrentPreset(channel);
		if (current - 1 < 0)
			current = instruments.getMaxPreset(bank);
		else
			current--;
		return progChange(channel, current);
	}

	public Midi instrumentDown() {
		return instrumentDown(0, 0);
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

	void sendCommand(String string) {
		log.debug("sendCommand: " + string);
		if (false == string.endsWith(NL))
			string = string + NL;

		try {
		    console.write((string).getBytes());
		    console.flush();
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

	public void toggle(Toggles type, boolean on) {
		if (type == Toggles.REVERB) {
			sendCommand( on ? "reverb on" : "reverb off");
			reverb = on;
		}
		if (type == Toggles.CHORUS) {
			sendCommand( on ? "chorus on" : "chorus off");
			chorus = on;
		}
	}

	public void toggle(Toggles type) {
		if (type == Toggles.REVERB) {
			toggleReverb();
		}
		if (type == Toggles.CHORUS) {
			toggleChorus();
		}
	}

	private void toggleReverb() {
		if (reverb)
			sendCommand("reverb off");
		else
			sendCommand("reverb on");
		reverb = !reverb;
	}

	private void toggleChorus() {
		if (chorus)
			sendCommand("chorus off");
		else
			sendCommand("chorus on");
		chorus = !chorus;
	}

	@Override
	public String getServiceName() {
		return FluidSynth.class.getSimpleName();
	}

	@Override
	public List<Command> getCommands() {

		return Collections.emptyList();
	}

	@Override
	public void execute(Command cmd, Properties props) throws Exception {
		// TODO
		throw new JudahException("not implemented yet.");
 	}

	@Override
	public void close() {
		try {
  	  		sendCommand(FluidCommand.QUIT);
  	  	} catch (JudahException e) { log.warn(e); }
	}

	@Override
	public Tab getGui() {
		return fluidWindow;
	}

	public void connect(JackClient jackclient, JackPort port) throws JackException {
		log.warn("Trying to connect " + port.getName() +" to " + MIDI_PORT);
	    Jack.getInstance().connect(jackclient, port.getName(), MIDI_PORT);
	}



}

