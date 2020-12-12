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

import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.api.MidiClient;
import net.judah.api.TimeListener;
import net.judah.api.TimeListener.Property;
import net.judah.api.TimeNotifier;
import net.judah.metronome.Metronome;
import net.judah.midi.JudahMidi;
import net.judah.sequencer.Sequencer.ControlMode;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JudahException;

@Log4j
public class SeqClock implements TimeNotifier {

	private final Sequencer seq;
	
	@Getter private final ArrayList<SeqData> sequences = new ArrayList<>();
	@Getter private MidiTracks tracks = new MidiTracks();
	@Getter private MidiTrack activeRecording;

	private final ArrayList<TimeListener> listeners = new ArrayList<>();
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final WakeUp wakeUp = new WakeUp();
	private ScheduledFuture<?> beeperHandle;

	private int count = 0;
	// private Integer duration = null;
	private int steps = 8;
	private int step = 0;
	
	// to speakers only
	private final MidiClient externalMidi;
	// to loopers
	private final JudahMidi internalMidi;
	
	// 8th-note step sequencer
	public SeqClock(Sequencer sequencer, int intro) throws InvalidMidiDataException {
		this(sequencer, intro, sequencer.getMeasure() * 2);
	}

	public SeqClock(Sequencer sequencer, int intro, int steps) throws InvalidMidiDataException {
		this.seq = sequencer;
		this.steps = steps;
		count = 0 - intro;
		// duration = (length == null) ? 0 : count + length;

		externalMidi = Metronome.getMidi();
		internalMidi = JudahZone.getMidi();
	}

	SeqClock(Sequencer seq) {
		this.seq = seq;
		addListener(seq);
		externalMidi = Metronome.getMidi();
		internalMidi = JudahZone.getMidi();
		steps = seq.getMeasure() * 2;
	}

	SeqClock(Sequencer seq, HashMap<String, Object> props) throws Exception {
		this(seq);
		seq.setTempo(Float.parseFloat("" + props.get(BPM)));
		seq.setMeasure(Integer.parseInt("" + props.get(MEASURE)));
		seq.setPulse(Integer.parseInt("" + props.get(Sequencer.PARAM_PULSE)));
		steps = seq.getMeasure() * 2;
		int intro = Integer.parseInt("" + props.get(SeqCommands.PARAM_INTRO));
		count = 0 - intro;
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

	class WakeUp implements Runnable {

		@Override
		public void run() {
//			if (changed.compareAndSet(true, false)) { // initilization or tempo update
//				
//				beeperHandle.cancel(true);
//				long period = Constants.millisPerBeat(seq.getTempo() * 2); // eighth notes
//		        beeperHandle = scheduler.scheduleAtFixedRate(
//		        		this, period, period, TimeUnit.MILLISECONDS);
//		        scheduler.schedule( new Runnable() {
//		        	@Override public void run() {
//		        		beeperHandle.cancel(true);}}, 12, TimeUnit.HOURS);
//			}
			chirp();
		}
	}

	public SeqData byName(String name) {
		for (SeqData sequence : getSequences())
			if (sequence.getName().equals(name))
				return sequence;
		Console.warn(name + " not found. current seqeunces: " + Arrays.toString(getSequences().toArray()));
		return null;
	}

	public void start() {

		if (beeperHandle != null)
			return;
		beeperHandle = scheduler.scheduleAtFixedRate(wakeUp, 0, Constants.millisPerBeat(seq.getTempo() * 2),
				TimeUnit.MILLISECONDS);
		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				beeperHandle.cancel(true);
			}
		}, 12, TimeUnit.HOURS);

	}

	public void stop() {
		if (beeperHandle == null)
			return;
		beeperHandle.cancel(true);
		beeperHandle = null;
		log.debug("internal shutdown");
		if (activeRecording != null)
			activeRecording.stop();
		tracks.forEach(track -> {
			track.stop();
		});
	}

	void chirp() {

		if (seq.getControl() == ControlMode.INTERNAL && step % 2 == 0) {
			if (count == 0)
				listeners.forEach(listener -> {
					listener.update(Property.TRANSPORT, JackTransportState.JackTransportStarting);
				});

			listeners.forEach(listener -> {
				listener.update(Property.BEAT, count);
			});
			count++;
		}

		for (SeqData s : sequences) {
			if (!s.isActive())
				continue;
			// TODO check loop count against beat count
			for (int i : s.getSequence())
				if (i == step)
					if (s.isRecord())
						internalMidi.queue(s.getNote());
					else
						externalMidi.queue(s.getNote());
		}
		step++;
		if (step == steps) {
			step = 0;
		}
//		chunks++;
//		if (chunks == seq.getPulse() / seq.getMeasure()) {
//			chunks = 0;
//			if (seq.getControl() == ControlMode.INTERNAL)
//				for (MidiTrack t : tracks) 
//					if (t.isActive())
//						t.play();
//		}

	}

	public void record(boolean active) {
		if (active) {
			if (activeRecording != null && !activeRecording.isEmpty()) {
				seq.getCommander().getListeners().remove(activeRecording);
				activeRecording.endRecord();
			}
			activeRecording = new MidiTrack(internalMidi);
			tracks.add(activeRecording);
			seq.getCommander().getListeners().add(activeRecording);
		} else if (activeRecording != null && !activeRecording.isEmpty()) {
			seq.getCommander().getListeners().remove(activeRecording);
			activeRecording.endRecord();
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
		if (active)
			track.play();
		else
			track.stop();

	}

	public void pulse(long previous) {

		boolean active = false;
		for (SeqData d : sequences)
			if (d.isActive()) {
				active = true;
				break;
			}
		if (active) {
			long now = System.currentTimeMillis();
			long duration = now - previous;

			long unit = (long) Math.floor(duration / (seq.getPulse() * 2));

			if (beeperHandle != null) {
				beeperHandle.cancel(true);
			}
			step = 0;
			count = 0;
			beeperHandle = scheduler.scheduleAtFixedRate(wakeUp, 0, unit, TimeUnit.MILLISECONDS);
			scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					beeperHandle.cancel(true);
					beeperHandle = null;
				}
			}, duration - 33, TimeUnit.MILLISECONDS);
		}

//		for (MidiTrack t : tracks) 
//			if (t.isActive()) 
//				t.play();
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
