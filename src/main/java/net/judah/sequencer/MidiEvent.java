package net.judah.sequencer;

import lombok.Data;
import net.judah.api.Midi;

@Data 
public class MidiEvent {

	private final Midi msg;
	private long offset;
	
	/** normalize offset to zero based from trackStart */
	public MidiEvent(Midi msg, long trackStart) {
		this.msg = new Midi(msg.getMessage());
		offset = System.currentTimeMillis() - trackStart;
	}

	/**@param unit time unit in milliseconds */
	public void quantize(long unit, long max) {
		long half = unit / 2;
		if (offset % unit == 0) return;
		if (offset % unit > half)
			offset = offset + unit - (offset % unit);
		else
			offset = offset - (offset % unit);
		if (offset >= max) offset = max - 1;
	}
	
}
