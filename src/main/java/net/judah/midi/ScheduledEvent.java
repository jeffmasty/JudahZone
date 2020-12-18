package net.judah.midi;

import lombok.Getter;
import net.judah.api.Midi;
import net.judah.sequencer.MidiEvent;
import net.judah.sequencer.MidiTrack;

public class ScheduledEvent extends MidiEvent {

	@Getter protected final MidiTrack owner;
	
	public ScheduledEvent(long frameNum, Midi msg, MidiTrack owner) {
		super(frameNum, msg);
		this.owner = owner;
	}

}
