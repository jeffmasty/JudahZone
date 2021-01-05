package net.judah.metronome;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.api.MidiQueue;

/** Volume adjusted javax midi sender */
@RequiredArgsConstructor
public class MidiReceiver implements Receiver {

	private final MidiQueue queue;
	@Setter private float gain = 1f;
	
	private ShortMessage current;

	@Override
	public void send(MidiMessage message, long timeStamp) {
		if (message instanceof ShortMessage && ((ShortMessage)message).getCommand() == ShortMessage.NOTE_ON) {
			current = (ShortMessage)message;
			try {
				current.setMessage(ShortMessage.NOTE_ON, current.getChannel(), current.getData1(), 
						Math.round(current.getData2() * gain));
			} catch (InvalidMidiDataException e) {
				System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
			}
		}
		queue.queue((ShortMessage)message);
	}

	@Override
	public void close() {
	}

}
