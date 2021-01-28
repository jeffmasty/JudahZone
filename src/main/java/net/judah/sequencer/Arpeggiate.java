package net.judah.sequencer;

import static net.judah.settings.Commands.OtherLbls.*;
import static net.judah.util.Constants.Param.*;

import java.util.HashMap;

import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.midi.MidiListener;

public class Arpeggiate extends Command implements MidiListener {

	public Arpeggiate() {
		super(ARPEGGIATE.name, ARPEGGIATE.desc, activeTemplate());
	}

	@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
		boolean active = false;
		if (midiData2 >= 0)
			active = midiData2 > 0;
		else
			active = Boolean.parseBoolean(props.get(ACTIVE).toString());
		if (active)
			seq.getListeners().add(this);
		else
			seq.getListeners().remove(this);
	}

	@Override
	public void feed(Midi midi) {
	    ((SeqClock)seq.getClock()).getSequences().queue(midi);
	}

	@Override
	public PassThrough getPassThroughMode() {
		return PassThrough.NOT_NOTES;
	}

}
