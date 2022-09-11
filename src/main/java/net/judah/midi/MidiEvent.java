package net.judah.midi;

import lombok.Data;
import net.judah.api.Midi;

@Data 
public class MidiEvent {

	protected final Midi msg;
	protected long offset;
	
	/** not normalized, offset time is JudahMidi frame*/
	public MidiEvent(long frameNum, Midi msg) {
		this.msg = msg;
		this.offset = frameNum;
	}
	
	/** normalize offset to zero based from trackStart */
	public MidiEvent(Midi msg, long trackStart) {
		this.msg = new Midi(msg.getMessage());
		offset = JudahMidi.getCurrent() - trackStart;
		assert offset >= 0 : "current " + JudahMidi.getCurrent() + " - trackStart " + trackStart + " = " + offset;
	}

	/**@param unit time unit to quantize to and max in milliseconds */
	public void quantize(long unit, long max) {
// TODO not gonna worry about it right now (moving to Jack Frames sync)		
//		long half = unit / 2;
//		if (offset % unit == 0) return;
//		if (offset % unit > half)
//			offset = offset + unit - (offset % unit);
//		else
//			offset = offset - (offset % unit);
//		if (offset >= max) offset = 0;
	}
	
}
