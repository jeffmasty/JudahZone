package net.judah;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.api.Service;
import net.judah.midi.MidiListener;
import net.judah.midi.MidiListener.PassThrough;
import net.judah.sequencer.Sequencer;
import net.judah.song.Link;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JudahException;
import net.judah.util.Links;

@RequiredArgsConstructor @Log4j 
public class CommandHandler {
	
	private final ArrayList<Command> available = new ArrayList<>();
	private final Links links = new Links();
	private final Sequencer sequencer;
	@Getter private final ArrayList<MidiListener> listeners = new ArrayList<>();
	
	/** call after all services have been initialized */
	public void initializeCommands() {
		for (Service s : sequencer.getServices()) 
			available.addAll(s.getCommands());

		for (Service s : JudahZone.getServices()) 
			available.addAll(s.getCommands());
		
		//log.debug("current commands: " + available.size()); for (Command c : available) log.debug("- " + c);
		available.sort((p1, p2) -> p1.getName().compareTo(p2.getName()));
	}

	public void addCommand(Command c) {
		available.add(c);
		available.sort((p1, p2) -> p1.getName().compareTo(p2.getName()));
	}
	
	/** @return true if consumed */
	public boolean midiProcessed(Midi midiMsg) {
		PassThrough mode = PassThrough.ALL;
		for (MidiListener listener : listeners) {
			new Thread() {
				@Override public void run() {
					listener.feed(midiMsg);};
			}.start();
			
			PassThrough current = listener.getPassThroughMode();
			if (current == PassThrough.NONE)
				mode = PassThrough.NONE;
			else if (current == PassThrough.NOTES && mode != PassThrough.NONE)
				mode = PassThrough.NOTES;
			else if (current == PassThrough.NOT_NOTES && mode != PassThrough.NONE)
				mode = PassThrough.NOT_NOTES;
		}
		if (PassThrough.NONE == mode)
			return true;
		else if (PassThrough.NOTES == mode)
			return !Midi.isNote(midiMsg);
		else if (PassThrough.NOT_NOTES == mode && Midi.isNote(midiMsg))
			return true;
		
		boolean result = false;
		HashMap<String, Object> p;
		for (Link mapping : links) {

			if (midiMsg.getCommand() == Midi.CONTROL_CHANGE 
					&& (byte)midiMsg.getData1() == mapping.getMidi()[1]) {
				p = new HashMap<String, Object>();
				p.putAll(mapping.getProps());
				fire(mapping, midiMsg.getData2());
				result = true;
			}
			
			else if (Arrays.equals(mapping.getMidi(), midiMsg.getMessage())) { // Prog Change
				fire(mapping, midiMsg.getData2());
				result = true;
			}
		}
		return result;
	}
	
	public void fire(Link mapping, int midiData2) {
		
		new Thread() {
			@Override public void run() {
				Command cmd = mapping.getCmd();
				try {
					if (cmd == null)
						throw new JudahException("Command not found for mapping. " + mapping);
					Console.info("cmdr@" + sequencer.getCount() + " execute: " 
							+ cmd + " " + midiData2 + " " + Constants.prettyPrint(mapping.getProps()));
					cmd.setSeq(sequencer);
					cmd.execute(mapping.getProps(), midiData2);
				} catch (Exception e) {
					Console.warn(e.getMessage() + " for " + cmd + " with " 
							+ Command.toString(mapping.getProps()), e);
				}
			}}.start();
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
	
	public void clearMappings() {
		links.clear();
	}
	
	public void addMappings(LinkedHashSet<Link> linkedHashSet) {
		links.addAll(linkedHashSet);
		log.debug("added " + linkedHashSet.size() + " mappings, total: " + links.size());
	}

}
