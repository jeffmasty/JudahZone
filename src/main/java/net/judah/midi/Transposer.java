package net.judah.midi;

import static net.judah.settings.Commands.OtherLbls.*;
import static net.judah.util.Constants.Param.*;

import java.util.HashMap;

import javax.sound.midi.InvalidMidiDataException;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.util.Console;
import net.judah.util.Constants;

@Log4j
public class Transposer extends Command {

	private JudahMidi midi;
	@Getter private boolean active = false;
	@Getter private int channel = 0;
	@Getter private int steps = -12;

	public Transposer(JudahMidi midi) {
		super(OCTAVER.name, OCTAVER.desc, transposeTemplate());
		this.midi = midi;
	}
	
	@Override
	public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
		if (midiData2 < 0) // sequencer
			try {active = Boolean.parseBoolean("" + props.get(ACTIVE));
			} catch(Throwable t) { Console.warn(t.getMessage(), t); }
		else // midi controller
			active = midiData2 > 0;
			
		if (!active) {
			midi.getRouter().removeOctaver();
			return;
		}
			
		try {
			channel = Integer.parseInt("" + props.get(CHANNEL));
			steps = Integer.parseInt("" + props.get(STEPS));
		} catch(Throwable t) { log.debug(t.getMessage()); }
			
		midi.getRouter().setOctaver(this);
	}

	public Midi process(Midi in) throws InvalidMidiDataException {
		if (Midi.isNote(in))
			return Constants.transpose(in, steps, channel);
		return in;
	}

}
