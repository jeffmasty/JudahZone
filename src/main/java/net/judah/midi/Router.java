package net.judah.midi;

import java.util.ArrayList;

import javax.sound.midi.InvalidMidiDataException;

import lombok.extern.log4j.Log4j;

@Log4j
public class Router extends ArrayList<Route> {

	int channel, command, data1;
	
	/** Realtime thread */
	public Midi process(Midi midi) {
		channel = midi.getChannel();
		command = midi.getCommand();
		data1 = midi.getData1();
		for (Route route : this) {
			if (route.getFromChannel() == channel) {
				if (route.isChannelRoute()) {
					try {
						return new Midi(midi.getCommand(), route.getToChannel(), midi.getData1(), midi.getData2());
					} catch (InvalidMidiDataException e) {
						log.error(e.getMessage() + " - " + midi + " - " + route, e);
					}
				}
				else if ( data1 == route.getFromData1()
						&& (route.getFromCommand() == Midi.NOTE_ON || route.getFromCommand() == Midi.NOTE_OFF) 
						&& (command == Midi.NOTE_ON || command == Midi.NOTE_OFF) ) {
					try {
						return new Midi(midi.getCommand(), route.getToChannel(), route.getToData1(), midi.getData2());
					} catch (InvalidMidiDataException e) {
						log.error(e.getMessage() + " - " + midi + " - " + route, e);					}
				}
				
			}
		}
		return midi;
	}
	

}
