package net.judah.midi;

import static net.judah.settings.Commands.OtherLbls.*;

import java.util.HashMap;

import javax.sound.midi.InvalidMidiDataException;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.util.Console;
import net.judah.util.Constants;

@Log4j
public class Octaver extends Command {

	private JudahMidi midi;
	@Getter private boolean active = false;
	@Getter private int channel = 0;
	@Getter private int steps = -12;

	public Octaver(JudahMidi midi) {
		super(OCTAVER.name, OCTAVER.desc, octaverTemplate());
		this.midi = midi;
	}
	
	private static final String PARAM_STEPS = "steps";
	
	private static HashMap<String, Class<?>> octaverTemplate() {
		HashMap<String, Class<?>> result = new HashMap<String, Class<?>>();
		result.put(PARAM_ACTIVE, Integer.class);
		result.put(PARAM_STEPS, Integer.class);
		result.put(Constants.PARAM_CHANNEL, Boolean.class);
		return result;
	}

	@Override
	public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
		if (midiData2 < 0) // sequencer
			try {active = Boolean.parseBoolean("" + props.get(PARAM_ACTIVE));
			} catch(Throwable t) { Console.warn(t.getMessage()); }
		else // midi controller
			active = midiData2 > 0;
			
		if (!active) {
			midi.getRouter().removeOctaver();
			return;
		}
			
		try {
			channel = Integer.parseInt("" + props.get(Constants.PARAM_CHANNEL));
			steps = Integer.parseInt("" + props.get(PARAM_STEPS));
		} catch(Throwable t) { log.debug(t.getMessage()); }
			
		midi.getRouter().setOctaver(this);
	}

	public Midi process(Midi in) throws InvalidMidiDataException {
		if (Midi.isNote(in))
			return new Midi(in.getCommand(), channel, in.getData1() + steps, in.getData2());
		return in;
	}

}
