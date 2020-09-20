package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;

/** Volume adjusted javax midi sender */
@RequiredArgsConstructor @Log4j
public class JudahReceiver implements Receiver {

	private final MidiClient midi;
	private float gain = 1f;
	
	private ShortMessage current;
	
	@Override
	public void send(MidiMessage message, long timeStamp) {
		if (message instanceof ShortMessage && ((ShortMessage)message).getCommand() == ShortMessage.NOTE_ON) {
			current = (ShortMessage)message;
			try {
				current.setMessage(ShortMessage.NOTE_ON, current.getChannel(), current.getData1(), 
						Math.round(current.getData2() * gain));
			} catch (InvalidMidiDataException e) {
				log.error(e.getMessage(), e);
			}
		}
		midi.queue((ShortMessage)message);
	}

	public void setGain(float gain) {
		this.gain = gain;
	}
	
	@Override
	public void close() {
	}

	public JudahReceiver() {
		this.midi = MidiClient.getInstance();
	}

}
