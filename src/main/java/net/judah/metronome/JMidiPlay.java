package net.judah.metronome;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;

import net.judah.midi.MidiClient;


/** plays a Midi file through the active fluid synth instance or creates one */
public class JMidiPlay {
	
	public static final File SOUND_FONT = new File("/usr/share/sounds/sf2/FluidR3_GM.sf2"); 

	public static final String SYNTH_LEFT = "fluidsynth-midi:left"; // "fluidsynth:l_00";
	public static final String SYNTH_RIGHT = "fluidsynth-midi:right"; // "fluidsynth:r_00";
	public static final String SYNTH_MIDI = "fluidsynth-midi:midi_00"; // "fluidsynth:midi";

	private MidiClient midi;
	private OutputStream outStream;
	private MidiPlayer player; 
	
	public JMidiPlay(File midiFile) throws InvalidMidiDataException, MidiUnavailableException, IOException, JackException {
		Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    	midi = new MidiClient(JMidiPlay.class.getSimpleName(), new String[] {}, new String[] {"out"});
    	JackClient client = midi.getJackclient();
	    Jack jack = Jack.getInstance();
		
		if (jack.getPorts(client, SYNTH_LEFT, null, null).length == 0) {
			String shellCommand = "fluidsynth" +
					" --midi-driver=jack --audio-driver=jack --portname=synth" +
		    		" -o synth.ladspa.active=0  --sample-rate " + client.getSampleRate() + " " +
					SOUND_FONT.getAbsolutePath();
			outStream = Runtime.getRuntime().exec(shellCommand).getOutputStream();
		}

    	while (jack.getPorts(client, SYNTH_LEFT, null, null).length == 0) 
	    	try {Thread.sleep(50);} catch (InterruptedException e) { }

    	if (!midi.getOutPorts().isEmpty())
    		jack.connect(client, midi.getOutPorts().get(0).getName(), SYNTH_MIDI);
    	try {
    		jack.connect(client, SYNTH_LEFT, "system:playback_1");
    		jack.connect(client, SYNTH_RIGHT, "system:playback_2");
    	} catch (Throwable t) {
    		// possibly already connected
    	}
    	
		player = new MidiPlayer(midiFile, 0, new MidiReceiver(midi));
		player.start();
		player.getSequencer();
		while (player.isRunning()) {
			// do nothing
		}
		close();
	}
	
	private void close() {
	    if (midi != null) {
			midi.close();
			midi = null;
	    }
		if (player != null) {
			if (player.isRunning())
				player.stop();
			player.close();
			player = null;
		}
	    if (outStream != null)
			try {
				// quits fluid synth
				outStream.write(("quit"+ System.getProperty("line.separator", "\r\n")).getBytes());
				outStream = null;
			} catch (Throwable e) {
				e.printStackTrace();
			}

	}

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			// open GUI
			System.out.println("No midi file");
			return;
		}
		try {
    		new JMidiPlay(new File(args[0]));
		} catch (Throwable e) {
			System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		System.out.println("exiting...");
		System.exit(0);
	}

	private class ShutdownHook extends Thread {
		@Override public void run() { close();}
	}

	
}
