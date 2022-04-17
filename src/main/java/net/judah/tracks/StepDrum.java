package net.judah.tracks;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import net.judah.beatbox.GMDrum;
import net.judah.beatbox.MidiBase;
import net.judah.beatbox.Sequence;

@RequiredArgsConstructor @Deprecated
public abstract class StepDrum {

	protected final MidiBase one;
	protected final MidiBase two;
	
	public StepDrum(GMDrum drum1, GMDrum drum2) {
		one = new MidiBase(drum1);
		two = new MidiBase(drum2);
	}
	
	public abstract Box getBeatBox();

	protected ArrayList<Sequence> doMe() {
		ArrayList<Sequence> result = new ArrayList<>();
		result.add(new Sequence(one));
		result.add(new Sequence(two));
		return result;
	}

}
