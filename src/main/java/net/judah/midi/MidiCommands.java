package net.judah.midi;

import static net.judah.settings.Commands.OtherLbls.*;
import static net.judah.util.Constants.Param.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import net.judah.JudahZone;
import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.util.Console;
import net.judah.util.JudahException;

public class MidiCommands extends ArrayList<Command> {
	
	public MidiCommands(JudahMidi midi) {
		add(new Transposer(midi));
		add(new Command(ROUTECHANNEL.name, ROUTECHANNEL.desc, channelTemplate()) {
		@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
			midi.routeChannel(props);}});
		add(new Command(MIDINOTE.name, MIDINOTE.desc, Midi.midiTemplate()) {
		@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
			midi.queue(Midi.fromProps(props));}});
		add(new Command(MIDIFILE.name, MIDIFILE.desc, playTemplate()) {
		@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
			boolean active = false;
			if (midiData2 > 0)
				active = true;
			else if (props.containsKey(ACTIVE))
				try { active = Boolean.parseBoolean("" + props.get(ACTIVE));
				} catch (Throwable t) { Console.debug(props.get(ACTIVE) + " " + t.getMessage());  }
			Object o = props.get(FILE);
			if (false == o instanceof String)  throw new JudahException("Missing midi file to play");
			File midi = new File(o.toString());
			if (!midi.isFile()) throw new JudahException("Not a midi file: " + o);
			if (active) { 
				JudahZone.getMetronome().setMidiFile(midi);
				JudahZone.getMetronome().play();
			}
			else
				JudahZone.getMetronome().stop();
		}
    });
	}
	
	public static HashMap<String, Class<?>> channelTemplate() {
		HashMap<String, Class<?>> result = new HashMap<String, Class<?>>();
		result.put("from", Integer.class);
		result.put("to", Integer.class);
		return result;
	}

	public static HashMap<String, Class<?>> playTemplate() {
		HashMap<String, Class<?>> result = new HashMap<String, Class<?>>();
		result.put(FILE, String.class);
		result.put(ACTIVE, Boolean.class);
		return result;
	}
	
}