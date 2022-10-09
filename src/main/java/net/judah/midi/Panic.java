package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import lombok.RequiredArgsConstructor;
import net.judah.api.MidiReceiver;
import net.judah.util.RTLogger;

@RequiredArgsConstructor
public class Panic extends Thread {

	private final MidiPort port;
	private int channel = 0;
	
	@Override
	public void run() {
		try {
			if (channel == 0)
				for (int i = 0; i < 128; i++) 
					port.send(new ShortMessage(ShortMessage.NOTE_OFF, i, 0), JudahMidi.ticker());
			else 
				for (int i = 0; i < 128; i++) 
					port.send(new ShortMessage(ShortMessage.NOTE_OFF, channel, i, 0), JudahMidi.ticker());
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(this, e);
		}
	}

	public Panic(MidiReceiver r, int channel) {
		this(r.getMidiPort(), channel);
	}
	
	public Panic(MidiPort port, int channel) {
		this.port = port;
		this.channel = channel;
	}
	
}
