package net.judah.fluid;

import static net.judah.util.Constants.NL;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.Program;
import net.judah.midi.Midi;
import net.judah.midi.MidiInstrument;
import net.judah.midi.MidiPort;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** runs fluid command line and connects to FluidSynth stdin and stdout ports */
public class FluidSynth extends MidiInstrument {
	public static final String LEFT_PORT = "fluidsynth-midi:left"; // "fluidsynth:l_00";
	public static final String RIGHT_PORT = "fluidsynth-midi:right"; // "fluidsynth:r_00";
	public static final String MIDI_PORT = "fluidsynth-midi:midi_00"; // "fluidsynth:midi";
	public static final File SOUND_FONT = new File("/usr/share/sounds/sf2/FluidR3_GM.sf2"); // "/usr/share/sounds/sf2/JJazzLab-SoundFont.sf2"
	public static final int CHANNELS = 4;
	
	private final String shellCommand;
	private Process process;
	private String[] changes = new String[CHANNELS];
	
	@Getter private final FluidChannels channels = new FluidChannels();
	private FluidListener listener;
	/** talks to FluidSynth on it's stdin */
	private OutputStream outStream;
	@Getter private final FluidConsole console;
	private float gain = 1f; // max 5.0

	public FluidSynth(int sampleRate, JackPort left, JackPort right, JackPort midi) {
		super(Constants.FLUID, LEFT_PORT, RIGHT_PORT, left, right, "Fluid.png");
		reverb = new FluidReverb(this); // use external reverb

		shellCommand = "fluidsynth" +
				" --midi-driver=jack --audio-driver=jack" +
				" -o synth.ladspa.active=0  --sample-rate " + sampleRate + " " +
				SOUND_FONT.getAbsolutePath();

		console = new FluidConsole(this);
		try {
			process = Runtime.getRuntime().exec(shellCommand);
		} catch (IOException e) {
			RTLogger.warn(this, e);
		}
		setMidiPort(new MidiPort(midi));
		new FluidListener(process.getErrorStream(), true).start();
		listener = new FluidListener(process.getInputStream(), false);
		listener.start();
		
		int delay = 200;
		Constants.sleep(delay);
		outStream = process.getOutputStream();
		Constants.sleep(delay);
		try {
			syncInstruments();
			syncChannels();
		} catch (Throwable t) {
			RTLogger.warn(this, "sync failed. " + t.getMessage());
		}
		sendCommand("chorus off");
		gain(gain);
	}

	public void syncChannels() {
		try {
			listener.sysOverride(FluidCommand.CHANNELS);
			outStream.write( (FluidCommand.CHANNELS.code + NL).getBytes() );
			outStream.flush();
			int count = 0;
			
			while (listener.sysOverride != null && count++ < 100) 
				Constants.sleep(25);
			
			if (listener.channels.isEmpty())
				throw new Exception("Error reading channels");
			else {
				channels.clear();
				for (FluidChannel c : listener.channels)
					channels.add(c);
			}
			for (int i = 0; i < changes.length; i++)
				if (channels.size() > i)
					changes[i] = channels.get(i).name;
			
		} catch (Exception e) { RTLogger.warn(this, e);  }
	}

	public void syncInstruments() throws Exception {
		listener.sysOverride(FluidCommand.INST);
		outStream.write((FluidCommand.INST.code + "1" + NL).getBytes());
		outStream.flush();
		int count = 0;
		while (listener.sysOverride != null && count++ < 35) 
			Thread.sleep(30);
		
		if (listener.instruments.isEmpty()) 
			throw new Exception("Fluid Error reading instruments");

		int size = listener.instruments.size();
		if (size > 99) // knob has 100 instruments
			size = 99;
		patches = new String[size];
		for (int i = 0; i < size; i++) 
			patches[i] = listener.instruments.get(i).name;
		RTLogger.log(this, "Loaded " + size + " instruments");
	}

	public Midi bankUp() {
		try {
			return new Midi(ShortMessage.CONTROL_CHANGE, 0, 0, 3);
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(this, e);
		}
		return null;
	}

	public Midi bankDown() {
		try {
			return new Midi(ShortMessage.CONTROL_CHANGE, 0, 0, 2);
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(this, e);
		}
		return null;
	}

	public void sendCommand(FluidCommand cmd) throws Exception {
		if (cmd.type != ValueType.NONE) {
			throw new Exception(cmd + " Command requires a value");
		}
		sendCommand(cmd.code);
	}

	void sendCommand(FluidCommand cmd, Object value) {
		String send = cmd.code + value;
		sendCommand(send);
	}

	public void sendCommand(String string) {
		if (false == string.endsWith(NL))
			string = string + NL;

		try {
			outStream.write((string).getBytes());
			outStream.flush();
		} catch(IOException e) {
			RTLogger.warn(this, e);
		}
	}

	public void mute() {
		sendCommand(FluidCommand.GAIN, FluidCommand.GAIN.min);
	}

	public void gain(float value) {
		if (value < 0) value = 0;
		if (value > 5) value = 5;
		sendCommand(FluidCommand.GAIN, value);
		this.gain = value;
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

	@Override
	public void close() {
		try {
			sendCommand(FluidCommand.QUIT);
		} catch (Exception e) { 
			RTLogger.warn(this, e);
		}
	}

	@Override
	public String getProg(int ch) {
		return changes[ch];
	}

	@Override public void progChange(String preset) {
		progChange(preset, 0);
	}

	@Override public void progChange(String preset, int ch) {
		for (int i = 0; i < patches.length; i++)
			if (patches[i].equals(preset)) {
				final int val = i;
				Constants.execute(() ->
					sendCommand(FluidCommand.PROG_CHANGE, ch + " " + val));
				// flooding: JudahMidi.queue(Midi.create(ShortMessage.PROGRAM_CHANGE, ch, i, 0), midiPort.getPort());
				if (ch < changes.length) 
					changes[ch] = preset;
				MainFrame.update(Program.first(this, ch)); 
			}
	}
	
}
