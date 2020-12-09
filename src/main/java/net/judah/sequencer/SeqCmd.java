package net.judah.sequencer;

import static net.judah.settings.Commands.SequencerLbls.*;
import static net.judah.util.Constants.*;

import java.util.HashMap;

import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.util.Constants;

public class SeqCmd extends Command {

	public static final String PARAM_LOOPS = "loop.count";
	public static final String PARAM_RECORD = "midi.record";
	public static final String PARAM_SEQ = "sequence";
	
	private SeqData sequence;
	
	public SeqCmd() {
		super(SEQ.name, SEQ.desc, template());
	}

	public static HashMap<String, Class<?>> template() {
		
		HashMap<String, Class<?>> result = Midi.midiTemplate();
		result.put(PARAM_NAME, String.class);
		result.putAll(Constants.active());
		result.put(PARAM_LOOPS, Integer.class);
		result.put(PARAM_RECORD, Boolean.class);
		result.put(PARAM_SEQ, String.class);
		return result;
	}

	@Override
	public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
		sequence = new SeqData(props);
		seq.getClock().getSequences().add(sequence);
	}
	
}
