package net.judah.sequencer;

import static net.judah.util.Constants.Param.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.InvalidMidiDataException;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.TimeListener;
import net.judah.api.TimeListener.Property;
import net.judah.api.TimeProvider;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiScheduler;
import net.judah.settings.Commands;
import net.judah.song.Link;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JudahException;

public class SeqClock extends Thread implements TimeProvider {
	
	private final Sequencer seq;
	private final MidiScheduler midischeduler = JudahMidi.getInstance().getScheduler();
	
	@Getter private final Steps sequences = new Steps();
	@Getter private MidiTracks tracks = new MidiTracks();
	@Getter private MidiTrack activeRecording;

	private final ArrayList<TimeListener> listeners = new ArrayList<>();
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> beeperHandle;

	@Getter private float tempo = 80f;
	@Setter @Getter private int measure = 4;

	private int count = -1;
	private int steps = 8;
	private int step = 0;
	private int loopCount = -1;
	private long msecLength = -1;
	private long frameLength = -1;
	
	/** jack frames since transport start or frames between pulses */
	@Getter private long lastPulse;
	
	SeqClock(Sequencer seq) {
		this.seq = seq;
		addListener(seq);
		steps = measure * 2;
	}

	public SeqClock(Sequencer sequencer, int intro, int steps) throws InvalidMidiDataException {
		this(sequencer);
		this.steps = steps;
		count = -1 - intro;
	}

	SeqClock(Sequencer seq, HashMap<String, Object> props) throws Exception {
		this(seq);
		tempo = Float.parseFloat("" + props.get(BPM));
		measure = Integer.parseInt("" + props.get(MEASURE));
		seq.setPulse(Integer.parseInt("" + props.get(Sequencer.PARAM_PULSE)));
		steps = measure * 2;
		int intro = Integer.parseInt("" + props.get(SeqCommands.PARAM_INTRO));
		count = -1 - intro;
	}

	@Override public void run() {
		// setup looping
		if (beeperHandle == null) {
			// lastPulse = JudahMidi.getCurrent();
			
			long period = Constants.millisPerBeat(tempo * 2);
			Console.addText("Internal Sequencer Clock starting with a bpm of " 
					+ getTempo() + " msec period of " + period);
			beeperHandle = scheduler.scheduleAtFixedRate(this, period, period, TimeUnit.MILLISECONDS);
			scheduler.schedule(new Runnable() {
				@Override public void run() {
					beeperHandle.cancel(true);}}, 
				12, TimeUnit.HOURS);
		}

		// run the current beat
		if (step % 2 == 0) {
			count++;

			// Console.info("step: " + step + " beat: " + step/2f + " count: " + count);
			listeners.forEach(listener -> {listener.update(Property.BEAT, count);});
			
			if (count % seq.getPulse() == 0) {
				loopCount++;
				lastPulse = JudahMidi.getCurrent();
				listeners.forEach(listener -> {listener.update(Property.LOOP, loopCount);});
				tracks.forEach( track -> {
					if (this == track.getTime())
						midischeduler.addTrack(lastPulse, track);});
				midischeduler.sort(midischeduler);
			}
		}
		sequences.process(step);

		step++;
		if (step == steps) {
			step = 0;
		}
	}

	public Step getSequence(String sequenceName) {
		for (Step sequence : getSequences())
			if (sequence.getName().equals(sequenceName))
				return sequence;
		Console.warn(sequenceName + " not found. current seqeunces: " 
				+ Arrays.toString(getSequences().toArray()), null);
		return null;
	}

	public void end() {
		if (beeperHandle == null)
			return;
		beeperHandle.cancel(true);
		beeperHandle = null;
		if (activeRecording != null)
			activeRecording.stop();
		tracks.forEach(track -> { track.stop();});
		midischeduler.clear();
		Console.info("clock end");
	}

	public void record(boolean active, boolean onLoop) {
		if (active) {
			if (activeRecording != null && !activeRecording.isEmpty()) {
				seq.getCommander().getListeners().remove(activeRecording);
			}
			
			activeRecording = (onLoop) ? 
				new MidiTrack(this) : new MidiTrack(lastPulse);
			if (!onLoop)
				for (Link l : seq.getSong().getLinks())
					if (l.getCmd().getName().equals(Commands.OtherLbls.ARPEGGIATE.name)) {
						// kludge: hookup the output of the track to transpose on the sequencer/arpeggiator
						activeRecording.setOutput(sequences);
					}
						
			tracks.add(activeRecording);
			seq.getCommander().getListeners().add(activeRecording);
		} else if (activeRecording != null && !activeRecording.isEmpty()) {
			seq.getCommander().getListeners().remove(activeRecording);
			activeRecording = null;
		}
	}

	public void play(int index, boolean active) throws JudahException {
		if (tracks.isEmpty())
			throw new JudahException("no tracks");
		if (index >= tracks.size())
			throw new JudahException("no such midi track: " + index + " " + Arrays.toString(tracks.toArray()));
		MidiTrack track = tracks.get(index);
		if (track.isActive() == active)
			return;
		if (active) {
			track.setActive(true);
			midischeduler.addTrack(lastPulse, track);
			midischeduler.sort(midischeduler);
		}
		else
			track.stop();
	}

	@Override
	public boolean setTempo(float tempo2) {
		if (tempo2 < tempo || tempo2 > tempo) {
			tempo = tempo2;
			if (JudahZone.getMetronome() != null)
				JudahZone.getMetronome().setTempo(tempo2);
		}
		return true;
	}

	@Override
	public void addListener(TimeListener l) {
		if (!listeners.contains(l))
			listeners.add(l);
	}

	@Override
	public void removeListener(TimeListener l) {
		listeners.remove(l);
	}


	public void pulse() {
		// Console.info("clock pulsed. now=" + JudahMidi.getCurrent() + " lastPulse = " + lastPulse);
		lastPulse = JudahMidi.getCurrent();
		
		for (MidiTrack t : tracks) 
		if (t.isActive() && this == t.getTime()) 
			midischeduler.addTrack(lastPulse, t);
		midischeduler.sort(midischeduler);

		boolean active = false;
		for (Step d : sequences)
			if (d.isActive()) {
				active = true;
				break;
			}
		if (active) {
			
			long unit = (long) Math.floor(msecLength / (seq.getPulse() * 2));

			if (beeperHandle != null) {
				beeperHandle.cancel(true);
			}
			step = 0;
			count = 0;
			beeperHandle = scheduler.scheduleAtFixedRate(this, 0, unit, TimeUnit.MILLISECONDS);
			scheduler.schedule(new Runnable() {
				@Override public void run() {
					beeperHandle.cancel(true);
					beeperHandle = null;}
			}, msecLength - 33, TimeUnit.MILLISECONDS);
		}
	}

	public void setRecordedLength(long recordedLength) {
		assert recordedLength != -1;
		this.msecLength = recordedLength;
	}

	public void setLength(long msecs, int frames) {
		assert msecs > 0;
		assert frames > 0;
		msecLength = msecs;
		frameLength = frames;
	}

	public long getNextPulse() {
		if (frameLength <= 0) assert false;
		return lastPulse + frameLength;
	}

}

//public static final int DEFAULT_DOWNBEAT = 34;
//public static final int DEFAULT_BEAT = 33;
//if (duration != null && count <= duration) {
//	boolean first = count % seq.getMeasure() == 0;
//	internalMidi.queue(first ? downbeat : beat);
////	if (count == duration) 
////		log.debug("tick tock shutting down");
////		try {// Metronome.setActive(false);
////		} catch (Exception e) { log.warn(e.getMessage(), e); }
//}
//
//if (bassThumps != null && count < bassThumps) {
//	internalMidi.queue(bassdrum);
//}
//public void changed() {
//changed.set(true);
//}
// void chirp2() {
//	chunks++;
//	if (chunks == seq.getPulse() / seq.getMeasure()) {
//		chunks = 0;
//		if (seq.getControl() == ControlMode.INTERNAL)
//			for (MidiTrack t : tracks) 
//				if (t.isActive())
//					t.play();
//	}}
