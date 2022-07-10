package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.RequiredArgsConstructor;
import net.judah.util.RTLogger;

@RequiredArgsConstructor
public class Panic extends Thread {

	private final JackPort port;
	private int channel = 0;
	
	@Override
	public void run() {
		try {
			if (channel == 0)
				for (int i = 0; i < 128; i++) 
					JudahMidi.queue(new ShortMessage(ShortMessage.NOTE_OFF, i, 0), port);
			else 
				for (int i = 0; i < 128; i++) 
					JudahMidi.queue(new ShortMessage(ShortMessage.NOTE_OFF, channel, i, 0), port);
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(this, e);
		}
	}

	public Panic(JackPort port, int channel) {
		this.port = port;
		this.channel = channel;
	}
	
}
