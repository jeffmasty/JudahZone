package net.judah.midi;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;

import lombok.extern.log4j.Log4j;
import net.judah.api.Midi;

@Log4j
public class Router extends ArrayList<Route> {

	protected int channel, command, data1;
	
	/** Realtime thread */
	public Midi process(Midi midi) {
		channel = midi.getChannel();
		command = midi.getCommand();
		data1 = midi.getData1();
		for (Route route : this) {
			if (route.getFromChannel() == channel) {
				if (route.isChannelRoute()) {
					try {
						if (route.getOctaver() == null)
							return new Midi(midi.getCommand(), route.getToChannel(), 
									midi.getData1(), midi.getData2());
						return route.getOctaver().process(midi);
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
	
	public void setOctaver(Transposer o) {
		add(new Route(o));
	}
	public void removeOctaver() {
		List<Route> octaver = new ArrayList<>();
		for (Route r : this)
			if (r.getOctaver() != null) 
				octaver.add(r);
		for (Route r : octaver)
			log.debug("Removed octaver route: " + remove(r));
	}
	
	

}
