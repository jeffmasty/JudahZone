package net.judah.seq.automation;

import javax.sound.midi.MidiEvent;

public record CCData(MidiEvent e, CC type) {}