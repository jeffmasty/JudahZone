package net.judah.tracker;

import java.util.HashMap;
import java.util.HashSet;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Midi;
import net.judah.midi.JudahMidi;
import net.judah.util.Constants;

// map of notes per step
public class Pattern extends HashMap<Integer, Notes> {

	public static final String PATTERN_TOKEN = "?@";
	
	@Getter @Setter String name;
	
	public Pattern(String name) {
		this.name = name;
	}
	
	public void noteOff(JackPort midiOut) {
		int ch = 0;
    	HashSet<Integer> off = new HashSet<>();
    	for (Integer i : keySet()) 
    		for (ShortMessage msg : get(i)) 
    			if (Midi.isNoteOn(msg)) {
    				ch = msg.getChannel();
     				off.add(msg.getData1());
    			}
    	for (int data1 : off) {
    		JudahMidi.queue(Midi.create(Midi.NOTE_OFF, ch, data1, 0), midiOut);
    	}
    			
	}
	
	public Notes get(int step, int data2) {
		if (get(step) == null)
			return null;
		for (ShortMessage msg : get(step)) {
			if (data2 == msg.getData1())
				return get(step);
		}
		return null;
	}
	
	public String forSave(boolean isDrums) {
		StringBuffer sb = new StringBuffer(PATTERN_TOKEN).append(name).append(Constants.NL);
		for (int step : keySet()) {
			if (get(step) == null)
				continue;
			for (Midi m : get(step))
				sb.append(step).append("/").append(m.toString()).append(Constants.NL);
		}
		return sb.toString();
	}
	
	private int num(String s) {
		return Integer.parseInt(s);
	}
	
	public void raw(String line) throws InvalidMidiDataException {	
		String[] split = line.split("/");
		int step = Integer.parseInt(split[0]);
		
		Midi midi = new Midi(num(split[1]), num(split[2]), num(split[3]), num(split[4]));
		
		Notes n = get(step);
		if (n == null) 
			put(step, new Notes(midi));
		else 
			n.add(midi);
	}
	
	
}
