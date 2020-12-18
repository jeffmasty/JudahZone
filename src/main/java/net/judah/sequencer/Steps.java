package net.judah.sequencer;

import java.util.ArrayList;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Midi;
import net.judah.api.MidiClient;
import net.judah.api.MidiQueue;
import net.judah.metronome.Metronome;
import net.judah.midi.JudahMidi;
import net.judah.util.Console;
import net.judah.util.Constants;

public class Steps extends ArrayList<Step> implements MidiQueue {
	
	/** to speakers only */
	@Setter private MidiClient externalMidi = Metronome.getMidi();
	/** to loopers */
	@Setter private JudahMidi internalMidi = JudahZone.getMidi();
	
	@Getter int translate = 0;

	public void process(int step) {
		try {
			for (Step s : this) {
				if (!s.isActive())
					continue;
				// TODO check loop count against beat count
				for (int i : s.getSequence())
					if (i == step) {
						Midi note = s.getNote();
						if (note.getChannel() != 9) {
							note = Constants.transpose(note, translate);
						}
						if (s.isRecord())
							internalMidi.queue(note);
						else
							externalMidi.queue(note);
					}
			}
		} catch (Throwable t) {
			Console.warn(step + ": " + t.getMessage(), t);
		}
		
	}

	@Override
	public void queue(ShortMessage midi) {
		if (midi == null || midi.getCommand() != Midi.NOTE_ON) return;
		translate = midi.getData1() - 36;
	}
	
}
