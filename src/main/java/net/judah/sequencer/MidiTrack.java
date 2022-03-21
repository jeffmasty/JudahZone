package net.judah.sequencer;

import java.util.ArrayList;
import java.util.Comparator;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.judah.api.Midi;
import net.judah.api.MidiQueue;
import net.judah.api.TimeProvider;
import net.judah.midi.MidiListener;

/** MidiEvents ordered by Jack Frame */
@Data @NoArgsConstructor @EqualsAndHashCode(callSuper = true) 
public class MidiTrack extends ArrayList<MidiEvent> implements MidiListener, Comparator<MidiEvent> {

	public static final long NORMALIZED = 0;
	
	/** in milliseconds */
	protected long length = -1;
	protected PassThrough passThroughMode = PassThrough.ALL;
	protected boolean active;
	protected float gain = 0.9f;
	protected int transpose = 0;
	protected Quantize quantize = Quantize.EIGHTH;
	protected TimeProvider time;
	protected Long referenceFrame;
	/** optional output routing */
	protected MidiQueue output;
	
	public MidiTrack(long reference) {
		this.referenceFrame = reference;  
		active = false;
	}
	
	public MidiTrack(TimeProvider time) {
		this.time = time;
	}
	
	public MidiTrack(TimeProvider time, PassThrough mode) {
		this(time);
		passThroughMode = mode;
	}
	
	public MidiTrack(TimeProvider time, PassThrough mode, Quantize q) {
		this(time, mode);
		quantize = q;
	}

	public boolean isPlaying() {
		return active;
	}
	
	@Override
	public void feed(Midi midi) {
		if (!Midi.isNote(midi)) return; 
		long origin = time == null ? referenceFrame : time.getLastPulse();
		
		add(new MidiEvent(midi, origin));
		sort(this);
	}
	
	public void stop() {
		active = false;
	}

	@Override
	public int compare(MidiEvent o1, MidiEvent o2) {
		if (o1.getOffset() == o2.getOffset()) return 0;
		return o1.getOffset() < o2.getOffset() ? -1 : 1;
	}

}
