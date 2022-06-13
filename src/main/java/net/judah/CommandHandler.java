package net.judah;

import java.util.ArrayList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.api.Command;
import net.judah.api.Service;
import net.judah.midi.MidiListener;
import net.judah.mixer.MixCommands;
import net.judah.sequencer.SeqCommands;

@RequiredArgsConstructor  
public class CommandHandler {
	
	private final ArrayList<Command> available = new ArrayList<>();
	@Getter private final ArrayList<MidiListener> listeners = new ArrayList<>();
	
	/** call after all services have been initialized */
	public void initializeCommands() {
		for (Service s : JudahZone.getServices()) 
			if (s.getCommands() != null)
				available.addAll(s.getCommands());
		available.addAll(new SeqCommands());
		available.addAll(new MixCommands());
		available.sort((p1, p2) -> p1.getName().compareTo(p2.getName()));
	}

	public void addCommand(Command c) {
		available.add(c);
		available.sort((p1, p2) -> p1.getName().compareTo(p2.getName()));
	}
	
	public Command[] getAvailableCommands() {
		return available.toArray(new Command[available.size()]);
	}

	public Command find(String name) {
		for (Command c : available)
			if (c.getName().equals(name))
				return c;
		return null;
	}

}
