package net.judah.util;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.judah.midi.JudahMidi;

/** Volume adjusted javax midi sender */
@NoArgsConstructor 
public class JackReceiver implements Receiver {

	@Setter @Getter private float gain = 1f;
	@Setter @Getter private JackPort midiOut;

	public JackReceiver(JackPort midiOut) {
		this.midiOut = midiOut;
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		if (message instanceof ShortMessage) {
			try {
				ShortMessage current = (ShortMessage)message;
				if (current.getCommand() == ShortMessage.NOTE_ON) {
				current.setMessage(ShortMessage.NOTE_ON, current.getChannel(), current.getData1(), 
						Math.round(current.getData2() * gain));
				}
				JudahMidi.queue(current, midiOut);
			} catch (Exception e) {
				RTLogger.warn(this, e);
			}
		}
	}

	@Override
	public void close() {
	}

}
