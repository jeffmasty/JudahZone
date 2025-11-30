package net.judah.synth.fluid;

import static net.judah.util.Constants.NL;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.Program;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.MidiInstrument;
import net.judah.omni.Threads;
import net.judah.seq.SynthRack;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** runs fluid command line and connects to FluidSynth stdin and stdout ports */
public final class FluidSynth extends MidiInstrument {
	public static final String LEFT_PORT = "fluidsynth-midi:left"; // "fluidsynth:l_00";
	public static final String RIGHT_PORT = "fluidsynth-midi:right"; // "fluidsynth:r_00";
	public static final String MIDI_PORT = "fluidsynth-midi:midi_00"; // "fluidsynth:midi";
	public static final File SOUND_FONT = new File("/usr/share/sounds/sf2/FluidR3_GM.sf2"); // "/home/judah/Downloads/JJazzLab-SoundFont.sf2"

	public record Drum(String name, int prog) {};
	private final String shellCommand;
	private Process process;
	/** talks to FluidSynth on stdin */
	private final OutputStream outStream;
	/** General-Midi presets */
	@Getter private final FluidChannels channels = new FluidChannels();
	@Getter private final ArrayList<Drum> drums = new ArrayList<>();
	private FluidListener listener;
//	private final String[] changes = new String[CHANNELS];

	public FluidSynth(JackPort midi, JackPort left, JackPort right) throws IOException {
		this(Constants.FLUID, midi, left, right, "Fluid.png");
	}

	public FluidSynth(String engineName, JackPort midi, JackPort left, JackPort right) throws IOException {
		this(engineName, midi, left, right, "Violin.png");
	}

	@SuppressWarnings("deprecation")
	private FluidSynth(String name, JackPort midi, JackPort left, JackPort right, String image) throws IOException {
		super(name, LEFT_PORT, RIGHT_PORT, left, right, image, midi);
		shellCommand = "fluidsynth -m jack -a jack -g 0.4 -r " + S_RATE + " " + SOUND_FONT.getAbsolutePath();

		process = Runtime.getRuntime().exec(shellCommand); // IOException
		new FluidListener(process.getErrorStream(), true).start();
		listener = new FluidListener(process.getInputStream(), false);
		listener.start();

		int delay = 222;
		Threads.sleep(delay);
		outStream = process.getOutputStream();

		FluidSynth[] predecessors = SynthRack.getFluids();
		if (predecessors.length == 0 || predecessors[0].getPatches().length == 0)
			try {
				Threads.sleep(2 * delay);
				syncInstruments();
				syncChannels();
			} catch (Throwable e) {
				RTLogger.warn(this, e);
			}
		else
			patches = predecessors[0].getPatches();

		replace(new FluidReverb(this)); // use external reverb
		sendCommand("chorus off");
		RTLogger.getParticipants().add(new FluidConsole(this));
	}


	public void syncChannels() {
		try {
			listener.sysOverride(FluidCommand.CHANNELS);
			outStream.write( (FluidCommand.CHANNELS.code + NL).getBytes() );
			outStream.flush();
			int count = 0;

			while (listener.sysOverride != null && count++ < 100)
				Threads.sleep(25);

			if (listener.channels.isEmpty())
				throw new Exception("Error reading channels");
			else {
				channels.clear();
				for (FluidChannel c : listener.channels)
					channels.add(c);
			}
//			for (int i = 0; i < changes.length; i++)
//				if (channels.size() > i)
//					changes[i] = channels.get(i).name;

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
		patches = new String[127];


		for (int i = 0; i < size; i++) {
			if (i < 127) // knob has 100 instruments
				patches[i] = listener.instruments.get(i).name;
			else if (listener.instruments.get(i).group == 128)
				drums.add(new Drum(listener.instruments.get(i).name, listener.instruments.get(i).index));
		}
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

	public void sendCommand(FluidCommand cmd, Object value) {
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

	@Override public void close() {
		try {
			sendCommand(FluidCommand.QUIT);
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
	}

	private void sendProg(int idx, int ch, String name) {
		JudahMidi.queue(Midi.create(ShortMessage.PROGRAM_CHANGE, ch, idx, 0), midiPort);
//		if (ch < changes.length)
//			changes[ch] = name;
		Program p = Program.first(tracks.get(ch));
		if (p != null) // TODO
			MainFrame.update(p);
	}

	public String getProg(int ch) {
		return tracks.get(ch).getState().getProgram();
	}

	public boolean progChange(String preset) {
		return progChange(preset, 0);
	}

	@Override public String progChange(int data2, int ch) {
		if (data2 < 0 || data2 > 127)
			return null;
		String name = data2 < patches.length ? patches[data2] : "?";
		sendProg(data2, ch, name);
		return name;
	}

	public boolean progChange(String preset, int ch) {
		try {
			int idx = Integer.parseInt(preset);
			if (idx > 0 && idx < 128)
				progChange(idx, ch);
			return true;
		} catch (NumberFormatException e) { /* ignore */ }

		for (int i = 0; i < patches.length; i++)
			if (patches[i].equals(preset)) {
				// flooding: Constants.execute(() -> {
				sendProg(i, ch, preset);
				return true;
			}
		return false;
	}


}

//public void mute() {
//sendCommand(FluidCommand.GAIN, FluidCommand.GAIN.min);
//}
//// TODO chorus routines to MIDI "SF2 default modulators" (too many jack hangs on stdin)
//public void chorusDelayLines(int val) {
//	FluidCommand delay = FluidCommand.CHORUS_DELAY_LINES;
//	if (val > delay.max.intValue()) val = delay.max.intValue();
//	if (val < delay.min.intValue()) val = delay.min.intValue();
//	sendCommand(delay.code + val); }
//public void chorusLevel(float val) {
//	FluidCommand level = FluidCommand.CHORUS_OUTPUT;
//	if (val > level.max.floatValue()) val = level.max.floatValue();
//	if (val < level.min.intValue()) val = level.min.intValue();
//	sendCommand(level.code + val); }
//public void chorusSpeed(float val) {
//	FluidCommand speed = FluidCommand.CHORUS_SPEED;
//	if (val > speed.max.floatValue()) val = speed.max.floatValue();
//	if (val < speed.min.floatValue()) val = speed.min.floatValue();
//	sendCommand(speed.code + val);}
//public void chorusDepth(int val) {
//	FluidCommand depth = FluidCommand.CHORUS_DEPTH;
//	if (val > depth.max.intValue()) val = depth.max.intValue();
//	if (val < depth.min.intValue()) val = depth.min.intValue();
//	sendCommand(depth.code + val); }
