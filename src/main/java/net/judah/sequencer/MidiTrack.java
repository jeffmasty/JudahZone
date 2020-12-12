package net.judah.sequencer;

import java.util.ArrayList;

import javax.sound.midi.InvalidMidiDataException;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;
import net.judah.api.Midi;
import net.judah.api.MidiQueue;
import net.judah.midi.MidiListener;
import net.judah.settings.MidiSetup;
import net.judah.util.Console;
import net.judah.util.Constants;

@Data @NoArgsConstructor @EqualsAndHashCode(callSuper = true) @Log4j
public class MidiTrack extends ArrayList<MidiEvent> implements Runnable, MidiListener {

	public static final long NORMALIZED = 0;
	
	private long creationTime = System.currentTimeMillis();
	private int startBeat;
	/** in milliseconds */
	private long length = -1;
	/** beats contained as recorded */
	private int beats;
	private PassThrough passThroughMode = PassThrough.ALL;
	private MidiQueue output;
	private long cycle;
	private boolean active;
	boolean onLoop = true;
	private float gain = 1f;
	private int transpose = 0;
	Quantize quantize = Quantize.EIGHTH;
	
	public MidiTrack(MidiQueue output) {
		this.output = output;
		startBeat = Sequencer.getCurrent().getCount();
	}
	
	public MidiTrack(MidiQueue output, PassThrough mode) {
		this(output);
		passThroughMode = mode;
	}
	
	public MidiTrack(MidiQueue output, PassThrough mode, Quantize q) {
		this(output, mode);
		quantize = q;
	}

	
	/** quantize and normalize */
	public void endRecord() {
		length = System.currentTimeMillis() - creationTime;
		creationTime = 0;
		beats = Sequencer.getCurrent().getCount() - startBeat;
		
		long unit;
		switch (quantize) {
			case QUARTER : unit = length / beats; break;
			case EIGHTH : unit = length / (beats * 2); break;
			case SIXTHEENTH : unit = length / (beats * 4); break;
			case NONE:
			default: return;
		}
		forEach(e -> { e.quantize(unit, length); });
	}

	public boolean isRecording() {
		return creationTime != NORMALIZED;			
	}
	
	public boolean isPlaying() {
		return output != null;
	}
	
	@Override
	public void feed(Midi midi) {
		add(new MidiEvent(midi, creationTime));
	}
	
	public void play() {
		new Thread(this).start();
		active = true;
	}
	
	public void stop() {
		output = null;
		active = false;
	}

	/** --thread interface-- */
	@Override
	public void run() {
		do {
			cycle = System.currentTimeMillis();
			try {
				for (MidiEvent event : this) { 
					if (nextEvent(event) == false)
						return;
				}
				if (onLoop)
					loop();
			} catch (Exception e) {
				output = null;
				log.error(e);
				Console.warn(e.getMessage());
			}
		} while (active && onLoop && output != null); 
	}

	
	private boolean nextEvent(MidiEvent event) throws InvalidMidiDataException {
		if (output == null) return false;
		long normalized = cycle + event.getOffset();
		long delta = normalized - System.currentTimeMillis();
		if (delta > 1)
			try {
				Thread.sleep(delta);
			} catch (InterruptedException e) { }
		if (event.getMsg().getChannel() == MidiSetup.DRUMS_CHANNEL) 
			output.queue(Constants.gain(event.getMsg(), gain));
		else 
			output.queue(Constants.transpose(event.getMsg(), transpose, gain));
		return true;
	}

	/** handle time between last event and length of track */
	private void loop() {
		long delta = cycle + length - System.currentTimeMillis();
		if (delta > 1)
			try {
				Thread.sleep(delta);
			} catch (InterruptedException e) { }
	}
	
}
