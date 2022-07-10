package net.judah.controllers;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Midi;
import net.judah.midi.JudahMidi;
import net.judah.util.RTLogger;

/** send Circuit Tracks MIDI1 and MIDI2 sequence data to various synths */
public class CircuitTracks implements Controller {
	
	@Getter @Setter private static JackPort out1;
	@Getter @Setter private static JackPort out2;
	
	private final int in1 = 2;  // Circuit sending channel
	private final int in2 = 3;  // Circuit sending channel
	
	@Override
	public boolean midiProcessed(Midi midi) {
		if (midi.getChannel() == in1)
			reroute(midi, out1);
		else if (midi.getChannel() == in2)
			reroute(midi, out2);
		return true;
	}

	private void reroute(Midi midi, JackPort out) {
		if (midi.getCommand() != ShortMessage.NOTE_ON && midi.getCommand() != ShortMessage.NOTE_OFF) {
			RTLogger.log(this, "discard " + midi); // EXPRESSION? 
			return;
		}
		try {
			midi.setMessage(midi.getCommand(), 0, midi.getData1(), midi.getData2());
			JackMidi.eventWrite(out, JudahMidi.getInstance().getTicker(), midi.getMessage(), midi.getLength());
		} catch (Exception e) {
			RTLogger.warn(this, midi + " --- " + e.getMessage());
		}
	}

}
