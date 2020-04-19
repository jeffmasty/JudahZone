package net.judah.midi;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JackReceiver implements Receiver {

	private final MidiClient midi;
	
	@Override
	public void send(MidiMessage message, long timeStamp) {
		midi.queue(message, timeStamp);
	}

	@Override
	public void close() {
	}

}
