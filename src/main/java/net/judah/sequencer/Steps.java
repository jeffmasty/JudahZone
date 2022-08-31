package net.judah.sequencer;

import java.util.ArrayList;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Midi;
import net.judah.midi.JudahMidi;
import net.judah.util.Console;
import net.judah.util.Constants;

public class Steps extends ArrayList<Step> implements MidiQueue {
	
	@Setter @Getter private JudahMidi midi = JudahZone.getMidi();

	@Getter @Setter int translate = 0;

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
						
						// if (s.isRecord())  midi.queue(note);
						// else externalMidi.queue(note);
						JudahMidi.queue(note, note.getChannel() == 9 ? midi.getCalfOut(): midi.getKeyboardSynth());
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
