package net.judah.api;

import java.io.Closeable;

import javax.sound.midi.ShortMessage;

import net.judah.midi.Midi;

public interface MidiClock extends Closeable {

	String PORT_NAME = "a2j:midiclock";

	int MIDI_24 = 24; // default track resolution/midi clock
	int CLOCK_SZ = 1; // bytes
	byte[] MIDI_START    = new byte[] {(byte)ShortMessage.START};
	byte[] MIDI_CONTINUE = new byte[] {(byte)ShortMessage.CONTINUE};
	byte[] MIDI_STOP     = new byte[] {(byte)ShortMessage.STOP};
	byte[] MIDI_CLOCK 	 = new byte[] {(byte)ShortMessage.TIMING_CLOCK};
    ShortMessage MIDI_TICK = new Midi(MIDI_CLOCK);

	boolean isInternal();

	void setTempo(float bpm);

	float getTempo();

	void start();

	void stop();

	void cont(); // continue

}