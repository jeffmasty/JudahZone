package net.judah.sequencer;
import static net.judah.sequencer.SeqCmd.*;
import static net.judah.util.Constants.Param.*;

import java.util.HashMap;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;
import net.judah.api.Midi;
import net.judah.util.Console;
import net.judah.util.Constants;

@Data @NoArgsConstructor @Log4j
public class Step {
	// sequence active= note=36 duration=16 type=inside notes=0,2.5
	private String name; 
	private boolean active = false;
	private Midi note;
	private Integer loops = 1;
	private boolean record = false;
	private int[] sequence = new int[] {1};
	
	public Step(HashMap<String, Object> props) {
		name = "" + props.get(NAME);
		try {
			String[] beats = ("" + props.get(SEQUENCE)).replace(" ", "").split(",");
			sequence = new int[beats.length];
			for (int i = 0; i < beats.length; i++)
				sequence[i] = Integer.parseInt(beats[i]);
		} catch (Throwable t) {
			log.warn(t.getMessage() + " " + props.get(SEQUENCE) + " " + note);
		}
		try {active = Boolean.parseBoolean("" + props.get(ACTIVE));
			} catch (Throwable t) { }
		try {
			note = Midi.fromProps(props);
		} catch (Throwable t) {log.debug(t.getMessage()); }
		try {loops = Integer.parseInt("" + props.get(PARAM_LOOPS));
			} catch (Throwable t) { }
		try {record = Boolean.parseBoolean("" + props.get(PARAM_RECORD));
		} catch (Throwable t) { }
	}
	
	public void setVolume(float gain) {
		try {note = new Midi(
				note.getCommand(), note.getChannel(), note.getData1(), Constants.gain2midi(gain));
		} catch (Throwable t) {
			Console.warn(t.getMessage() + " for gain: " + gain, t);
		}
	}
	
}
