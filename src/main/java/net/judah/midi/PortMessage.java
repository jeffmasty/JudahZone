package net.judah.midi;

import javax.sound.midi.MidiMessage;

import org.jaudiolibs.jnajack.JackPort;

public record PortMessage (MidiMessage midi, JackPort port) {

}
