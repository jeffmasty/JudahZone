package net.judah.metronome;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackMidi;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.beatbox.BeatBox;
import net.judah.util.RTLogger;

/** Volume adjusted javax midi sender */
@RequiredArgsConstructor
public class JackReceiver implements Receiver {

	@Setter private float gain = 1f;
	private ShortMessage current;
	private final BeatBox midiOut;
	
	@Override
	public void send(MidiMessage message, long timeStamp) {
		if (message instanceof ShortMessage) {
			try {
				current = (ShortMessage)message;
				if (current.getCommand() == ShortMessage.NOTE_ON) {
				current.setMessage(ShortMessage.NOTE_ON, current.getChannel(), current.getData1(), 
						Math.round(current.getData2() * gain));
				}
				JackMidi.eventWrite(midiOut.getMidiOut(), 0, current.getMessage(), current.getLength());
			} catch (Exception e) {
				RTLogger.warn(this, e);
			}
		}
	}

	@Override
	public void close() {
	}

}
