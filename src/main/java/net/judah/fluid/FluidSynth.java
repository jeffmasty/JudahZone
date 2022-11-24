package net.judah.fluid;

import static net.judah.util.Constants.NL;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackException;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Midi;
import net.judah.api.MidiPatch;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiPort;
import net.judah.midi.ProgMsg;
import net.judah.mixer.MidiInstrument;
import net.judah.util.Constants;
import net.judah.util.FluidConsole;
import net.judah.util.JudahException;
import net.judah.util.RTLogger;

/** runs fluid command line and connects to FluidSynth stdin and stdout ports */
public class FluidSynth extends MidiInstrument {
	public static final String NAME = "Fluid";
	public static final String LEFT_PORT = "fluidsynth-midi:left"; // "fluidsynth:l_00";
	public static final String RIGHT_PORT = "fluidsynth-midi:right"; // "fluidsynth:r_00";
	public static final String MIDI_PORT = "fluidsynth-midi:midi_00"; // "fluidsynth:midi";
	public static final File SOUND_FONT = new File("/usr/share/sounds/sf2/FluidR3_GM.sf2"); // "/usr/share/sounds/sf2/JJazzLab-SoundFont.sf2"

	private final String shellCommand;
	private Process process;

	private FluidListener listener;
	/** talks to FluidSynth on it's stdin */
	private OutputStream outStream;

	@Getter private final FluidConsole console;

	@Getter static Instruments instruments = new Instruments();
	@Getter private String[] patches;
	@Getter private final Channels channels = new Channels();

	private float gain = 3f; // max 5.0

	public FluidSynth (int sampleRate, boolean startListeners, MidiPort port) throws JackException, JudahException, IOException {
		this(sampleRate, SOUND_FONT, startListeners, port);
	}

	public FluidSynth (int sampleRate, MidiPort port) throws JackException, JudahException, IOException {
		this(sampleRate, SOUND_FONT, true, port);
	}

	public FluidSynth (int sampleRate, File soundFont, boolean startListeners, MidiPort port) throws JackException, JudahException, IOException {
		super(NAME, LEFT_PORT, RIGHT_PORT, port, "Fluid.png");
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

		int delay = 111;
		Constants.sleep(delay * 2);
		
		// read FluidSynth output
		listener = new FluidListener(process.getInputStream(), false);
		new FluidListener(process.getErrorStream(), true).start();
		if (startListeners)
			listener.start();
		Constants.sleep(delay);
		outStream = process.getOutputStream();

        sendCommand("chorus off");
		Constants.sleep(delay);
        gain(gain);
		Constants.sleep(delay);

		if (startListeners)
			try {
				syncChannels();
				syncInstruments();
			} catch (Throwable t) {
				RTLogger.warn(this, "sync failed. " + t.getMessage());
			}
		
        RTLogger.log(this, "FluidSynth channels: " + channels.size() + " instruments: " + instruments.size());
        
        reverb = new FluidReverb(this); // use external reverb
	}

	public void syncChannels() throws InterruptedException, IOException, JudahException {
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

	public void syncInstruments() throws InterruptedException, IOException, JudahException {
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
		patches = new String[listener.instruments.size()];
		for (int i = 0; i < listener.instruments.size(); i++) {
			instruments.add(listener.instruments.get(i));
			patches[i ]= instruments.get(i).name;
		}
		
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

	/**The MIDI message used to specify the instrument has one STATUS byte and one DATA byte :
        Status byte : 1100 CCCC
        Data byte 1 : 0XXX XXXX
    where CCCC is the MIDI channel (0 to 15) and XXXXXXX is the instrument number from 0 to 127. */
	public Midi progChange2(final int channel, int preset) {
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
						RTLogger.log(this, "PROG CHNG " + instruments.get(after) + " (from " + instruments.get(before) + ")");
					} catch (Throwable e) { 			
						RTLogger.warn(this, e);
					}
				};
			}.start();

			return msg;
		} catch (Throwable t) {
			RTLogger.warn(this, t);
			return null;
		}
	}

	public void instUp(int channel, boolean up) {
		int current = channels.getCurrentPreset(channel);
		int bank = channels.getBank(channel);
		int preset = instruments.getNextPreset(bank, current, up);
		JudahZone.getMidiGui().getFluidProg()
				.setSelectedIndex(preset);
	}

	@SuppressWarnings("unused")
	private int progChangeSync(int channel, int preset) {
		try {
			JudahZone.getMidiGui().getFluidProg()
					.setSelectedIndex(preset);
			
			syncChannels();
			int result = channels.getCurrentPreset(channel);
			int bank = channels.getBank(channel);
			for (MidiPatch f : instruments)
				if (f.group == bank && f.index == preset)
					RTLogger.log(this, f.toString());
			return result;
		} catch (Throwable t) {
			RTLogger.warn(this, t);
			return -1;
		}
	}

	public void sendCommand(FluidCommand cmd) throws JudahException {
		if (cmd.type != ValueType.NONE) {
			throw new JudahException(cmd + " Command requires a value");
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
		} catch (JudahException e) { 
			RTLogger.warn(this, e);
		}
	}

	public static class Instruments extends ArrayList<MidiPatch> {
	    ArrayList<MidiPatch> drums;
	    ArrayList<MidiPatch> instruments;

	    public int lookupKit(String name) {
	    	for (MidiPatch i : getDrumkits())
	    		if (i.name.equals(name))
	    			return i.index;
	    	throw new InvalidParameterException(name);
	    }
	    
	    /**@return bank 128 */
	    public ArrayList<MidiPatch> getDrumkits() {
	        if (drums != null) return drums;
	        drums = new ArrayList<>();
	        for (MidiPatch i : this)
	            if (i.group == 128) drums.add(i);
	        return drums;
	    }
	    /**@return bank 1*/
	    public ArrayList<MidiPatch> getInstruments() {
	        if (instruments != null) return instruments;
	        instruments = new ArrayList<>();
	        for (MidiPatch i : this)
	            if (i.group == 0) instruments.add(i);
	        return instruments;
	    }


		public int getNextPreset(int bank, int current, boolean up) {
			int index = -1;
			int count = -1;
			// extract bank
			ArrayList<MidiPatch> roll = new ArrayList<>();
			for (MidiPatch i : this)
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
	public int getProg(int ch) {
		return channels.getCurrentPreset(ch);
	}

	@Override public void progChange(String preset) {
		progChange(preset, 0);
	}

	@Override public void progChange(String preset, int ch) {
		for (int i = 0; i < patches.length; i++)
			if (patches[i].equals(preset)) {
				int idx = instruments.getInstruments().get(i).index;
				try {
					Midi midi = new Midi(ShortMessage.PROGRAM_CHANGE, ch, idx);
					JudahMidi.queue(midi, midiPort.getPort());
					syncChannels();
				} catch (Exception e) {
					RTLogger.warn(this, e);
				}
			}
	}
	
	
}
