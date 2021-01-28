package net.judah.sequencer;

import static net.judah.settings.Commands.SequencerLbls.*;
import static net.judah.util.Constants.Param.*;

import java.util.HashMap;

import net.judah.api.Command;
import net.judah.api.Midi;

public class SeqCmd extends Command {

	public static final String PARAM_LOOPS = "loop.count";
	/** internal vs. external synth */
	public static final String PARAM_RECORD = "midi.record";

	private Step sequence;

	public SeqCmd() {
		super(SEQ.name, SEQ.desc, template());
	}

	public static HashMap<String, Class<?>> template() {

		HashMap<String, Class<?>> result = Midi.midiTemplate();
		result.put(NAME, String.class);
		result.putAll(activeTemplate());
		result.put(PARAM_RECORD, Boolean.class);
		result.put(SEQUENCE, String.class);
		// result.put(PARAM_LOOPS, Integer.class);
		return result;
	}

	@Override
	public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
		sequence = new Step(props);
		((SeqClock)seq.getClock()).getSequences().add(sequence);
	}

}
