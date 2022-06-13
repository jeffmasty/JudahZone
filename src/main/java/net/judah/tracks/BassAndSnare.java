package net.judah.tracks;

import static net.judah.beatbox.GMDrum.*;

import java.util.ArrayList;

import net.judah.beatbox.Beat;
import net.judah.beatbox.Sequence;

@Deprecated
public class BassAndSnare extends StepDrum {

	 public BassAndSnare() {
		 super(BassDrum, AcousticSnare);
	 }

	@Override
	public Box getBeatBox() {
		Box result = new Box();
		
		ArrayList<Sequence> current;
		Sequence bass, snare;
		
		// pattern A
		current = doMe();
		bass = current.get(0);
		snare = current.get(1);
		bass.add(new Beat(0));
		bass.add(new Beat(8));
		snare.add(new Beat(4));
		snare.add(new Beat(12));
		result.add(current);
		
		// pattern B
		current = doMe();
		bass = current.get(0);
		snare = current.get(1);
		bass.add(new Beat(0));
		bass.add(new Beat(8));
		bass.add(new Beat(11));
		bass.add(new Beat(13));
		snare.add(new Beat(4));
		snare.add(new Beat(12));
		result.add(current);

		// pattern C
		current = doMe();
		bass = current.get(0);
		snare = current.get(1);
		bass.add(new Beat(0));
		bass.add(new Beat(8));
		bass.add(new Beat(13));
		snare.add(new Beat(4));
		snare.add(new Beat(12));
		snare.add(new Beat(13));
		snare.add(new Beat(15));
		result.add(current);

		// pattern D
		current = doMe();
		bass = current.get(0);
		snare = current.get(1);
		bass.add(new Beat(0));
		bass.add(new Beat(4));
		bass.add(new Beat(8));
		snare.add(new Beat(12));
		snare.add(new Beat(4));
		snare.add(new Beat(12));
		snare.add(new Beat(15));
		result.add(current);

		
		
		return result;
	}
	
}
